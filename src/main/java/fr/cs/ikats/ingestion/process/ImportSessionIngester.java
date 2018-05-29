/**
 * Copyright 2018 CS Systèmes d'Information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.ikats.ingestion.process;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.common.dao.exception.IkatsDaoConflictException;
import fr.cs.ikats.common.dao.exception.IkatsDaoException;
import fr.cs.ikats.ingestion.Configuration;
import fr.cs.ikats.ingestion.IngestionConfig;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.metadata.MetaDataFacade;
import fr.cs.ikats.metadata.model.FunctionalIdentifier;
import fr.cs.ikats.metadata.model.MetaData;
import fr.cs.ikats.metadata.model.MetaData.MetaType;
import fr.cs.ikats.ts.dataset.DataSetFacade;
import fr.cs.ikats.util.concurrent.ExecutorPoolManager;

/**
 * That class represent a thread which is the main part of the ingestion process ({@link IngestionProcess}) that basically submit all items/timeseries ({@link ImportItem}) of the dataset for ingestion and analyse/check their individual status.
 */
@Stateless
public class ImportSessionIngester implements Runnable {

	/** The session on which to work */
	private ImportSession session;

	/** References the caller process */
	private IngestionProcess process;

	/** Factory that creates task for low level import (currently OpenTSDB) */
	private ImportItemTaskFactory importItemTaskFactory;

	/** Synchronized list of ({@link Future}) tasks */
	private List<Future<ImportItem>> submitedTasks = Collections.synchronizedList(new ArrayList<Future<ImportItem>>());
	
	private HashMap<String, ImportItem>tsuidToRegister = new HashMap<String, ImportItem>();

	private MetaDataFacade metaDataFacade;

	private Logger logger = LoggerFactory.getLogger(ImportSessionIngester.class);
	
	@SuppressWarnings("unused")
	private ImportSessionIngester() {
		
	}
	
	/**
	 * Create a "session ingester" to import/ingest an {@link ImportSession} with link to an {@link IngestionProcess}.
	 * 
	 * @param ingestionProcess Provides services : the {@link DataSetFacade} to register the TS into the dataset and the {@link ExecutorPoolManager} instance to submit ingestion tasks. 
	 * @param session Properties of the ingestion
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ImportSessionIngester(IngestionProcess ingestionProcess, ImportSession session) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		this.process = ingestionProcess;
		this.session = session;

		// Get the factory to import session items. The default test implementation is used if none found.
		String taskFactoryFQN = session.getImporter();
		if (taskFactoryFQN == null) {
			taskFactoryFQN = Configuration.getInstance().getString(IngestionConfig.IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY);
		}
		
		// Review#147170 nettoyer code commenté ou retablir ...
//		String taskFactoryName = taskFactoryFQN.substring(taskFactoryFQN.lastIndexOf('.') + 1);
		
//		Context ctx = new InitialContext();
//		importItemTaskFactory = (ImportItemTaskFactory) ctx.lookup("java:global/ikats-ingestion/" + taskFactoryName);
		
//		Class<?> importItemTaskFactoryClazz = Class.forName(taskFactoryFQN, false, this.getClass().getClassLoader()); 
		Class<?> importItemTaskFactoryClazz = getClass().getClassLoader().loadClass(taskFactoryFQN); 
		importItemTaskFactory = (ImportItemTaskFactory) importItemTaskFactoryClazz.newInstance();
		logger.info("ImportItemTaskFactory injected as {}", importItemTaskFactory.getClass().getName());
		
		// Instance the facade for metadata creation 
		metaDataFacade = new MetaDataFacade();

	}

	/**
	 * The main objective of that thread is to run a loop that submit an import task for each {@link ImportItem}.<br>
	 * 
	 * <p>
	 * There are 2 nested loops : <br>
	 *   <ol>
	 *     <li>First loop runs until :
	 *       <ul>
	 *         <li>The session is marked as {@link ImportStatus#RUNNING}
	 *         <li>There are yet items to ingest/import.<br>
	 *           <ul>
	 *             <li>The list of items to ingest/import is unstacked at each item imported; 
	 *           </ul>
	 *       </ul>
	 *     <li>The second loop (nested) is in charge of trying to submit an ingestion task.<br>
	 *     The task is created with an implementation of the factory {@link ImportItemTaskFactory}<br>
	 *     The task is submitted to the pool which is an instance of {@link ExecutorPoolManager}, a fixed size pool (using a {@link ArrayBlockingQueue}.<br> 
	 *     (Note: The queue if configured to run a maximum tasks at time, see {@link ExecutorPoolManager#workingQueueSize}, currently at 10.<br>
	 *     So the code do the following:
	 *     <ul>
	 *       <li>Gets the last version of the list of {@link ImportSession#getItemsToImport() itemsToImport}
	 *       <li>LOOP UNTIL there is one item in that list
	 *       <ul>
	 *         <li>IF the {@link ImportItem} is in {@link ImportStatus#CREATED CREATED} state
	 *         <ul>
	 *           <li>submit a task (that will change the state of the {@link ImportItem}) and return a {@link Future} of {@link ImportItem}<br>
	 *           <li>IF the return is not null, add it to the {@link ImportSessionIngester#submitedTasks submitedTasks} list.<br>
	 *           That list will be unstacked by the inner {@link ImportItemAnalyserThread}
	 *           when an item is imported the analyser thread calls {@link ImportItem#setItemImported()} which will remove it from the list of {@link ImportSession#getItemsToImport() itemsToImport}
	 *           <li>IF the return is null, assume that the task was not submitted (due to queue full)<br>
	 *           reset the {@link ImportItem} state to {@link ImportStatus#CREATED CREATED}
	 *         </ul>
	 *       </ul>
	 *     </ul>
	 *   </ol> 
	 * </p>
	 */
	public void run() {
		
		
		// Management of 'a posteriori' robustness.
		// Do a cleaning pass in the items lists for sessions that were aborted
		session.setStatus(ImportStatus.CLEANSING_PASSES);
		itemsImportedCleaningPass();
		itemsToImportCleaningPass();
		session.setStatus(ImportStatus.RUNNING);

		// Prepare the stats 
		session.getStats().timestampIngestion(true);
		session.setStartDate(session.getStats().getDateIngestionStarted());
		
		// Launch import results analyser thread
		logger.info("Starting ingestion of {} items for dataset {}", session.getItemsToImport().size(), session.getDataset());
	    
	    // Review#147170 c'est un runnable pas un thread ... renommer en importItemAnalyserRunnable ?
		ImportItemAnalyserThread importItemAnalyserThread = new ImportItemAnalyserThread();
		
		// Review#147170 renommer en qqch de plus explicite que 'thread' : importItemAnalyserThread ? ...
		Thread thread = new Thread(importItemAnalyserThread);
		thread.start();

		// Launch the import loop

        // Review#147170 cf remarque entete de ImportItemAnalyserThread ... la machine a etats pourrait evoluer ...
        // Review#147170 ... vers if ( CREATED) ... else if ( IMPORTED ) ... 
        // Review#147170 expliciter le but de 'session.getStatus() == ImportStatus.RUNNING' en lien avec IngestionProcess
		
		// Review#147170 merge fait MBD: condition revue par FTL: while revu: parait plus clair
		// Review#147170 commentaire "FIXME" a terminer:
		// FIXME condition de fin de boucle à optimiser pour rendreC
		while (session.getStatus() == ImportStatus.RUNNING && session.getItemsToImport().size() > 0) {

			// at each loop get the last list of items to import
			ListIterator<ImportItem> listIterator = session.getItemsToImport().listIterator();
			while (listIterator.hasNext()) {
				
				ImportItem importItem = (ImportItem) listIterator.next();

				// for each one create and submit and import task
				if (importItem.getStatus() == ImportStatus.CREATED) { 
				    
				    // Review#147170 expliquer plus le lien entre interface importItemTaskFactory 
				    // Review#147170 et l'implem de createTask initialisée
					Callable<ImportItem> task = importItemTaskFactory.createTask(importItem);
					Future<ImportItem> submitedTask = process.getExecutorPool().submit(task);
					
					if (submitedTask != null) {
						// Add the future result to the results stack in
						// synchronized mode to avoid concurrency caveats
						synchronized (submitedTasks) {
							submitedTasks.add(submitedTask);
						}
					}
					else {
						// Reset import item status
						importItem.setStatus(ImportStatus.CREATED);
						// do not try to loop again : we can't submit tasks
						break;
					}
				}
			}
			
			try {
				// Wait a moment before looping again.
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				// TODO manage error ?
			    // Review#147170 prise en compte TODO 
				logger.warn("Interrupted while waiting", ie);
			}
		}
		
		// launch stop command to the result analysis thread and wait for it to
		// finish
		// Review#147170 conflit entre l ecriture de state par le thread et ce stop() sur le runnable ?
		// Review#147170 on pourrait prendre une precaution de synchro ?
		importItemAnalyserThread.stop();
		try {
			// Wait for analyser to finish
			thread.join();
		} 
		catch (InterruptedException ie) {
			logger.error("Interrupted while waiting importItemAnalyserThread to finish", ie);
		}
		finally {
			session.getStats().timestampIngestion(false);
			session.setEndDate(session.getStats().getDateIngestionCompleted());
			
			// set ingest session final state
			if (importItemAnalyserThread.state == ImportItemAnalyserState.COMPLETED) {
				session.setStatus(ImportStatus.COMPLETED);
			} else {
				session.addError("The submitted tasks are not fully analysed, the analyser thread finished with state: " + importItemAnalyserThread.state.name());
				session.setStatus(ImportStatus.ERROR);
			}
		}

	}

	/**
	 * Do a pass on the {@link ImportSession#getItemsToImport()} list to clean the list depending on the item {@link ImportStatus}
	 */
	private void itemsImportedCleaningPass() {
		
		Object[] items = session.getItemsImported().toArray();
		
		Instant startDate = Instant.now();
		logger.info("Doing cleaning pass on the items imported list for {} items...", items.length);
		
		for (int i = 0; i < items.length; i++) {
			ImportItem importItem = (ImportItem) items[i];
			
			// Check the import item status and move item from toImport list 
			// to one of the completed or erroneous list
			switch (importItem.getStatus()) {
			case CREATED:
				// Do nothing, the item will be processed
				break;
			case ANALYSED:
			case RUNNING:
			case ERROR: // For erroneous items, as we restart a session, reset them 
				// Reset to CREATED
				logger.warn("Item {} state resetted to CREATED (Old state: {})", importItem.getFuncId(), importItem.getStatus());
				importItem.setStatus(ImportStatus.CREATED);
				break;
			case IMPORTED:
				// move the item as imported in the session only if import is completed
				logger.warn("Item {} in state IMPORTED, database registration will be completed.", importItem.getFuncId());
				registerFunctionalIdent(importItem);
				registerMetadata(importItem);
				registerItemInDataset(importItem);
				break;
			case COMPLETED:
				// Do nothing, the item is fully completed
				break;
			case CANCELLED:
			default:
				// move the item in the errors stack
				logger.error("Item {} with state {}, put into the items in error list.", importItem.getFuncId(), importItem.getStatus());
				importItem.setItemInError();
				break;
			}
		}
		
		logger.info("Cleaning pass on the items imported list done in {}", Duration.between(startDate, Instant.now()).toString());
	}

	/**
	 * Do a pass on the {@link ImportSession#getItemsToImport()} list to clean the list depending on the item {@link ImportStatus}
	 */
	private void itemsToImportCleaningPass() {
		
		Object[] items = session.getItemsToImport().toArray();
		
		Instant startDate = Instant.now();
		logger.info("Doing cleaning pass on the items to import list for {} items...", items.length);

		for (int i = 0; i < items.length; i++) {
			ImportItem importItem = (ImportItem) items[i];
			
			// Check the import item status and move item from toImport list 
			// to one of the completed or erroneous list
			switch (importItem.getStatus()) {
			case CREATED:
				// Do nothing, the item will be processed
				break;
			case ANALYSED:
			case RUNNING:
			case ERROR:
				// Reset to CREATED
				logger.warn("Item {} state resetted to CREATED (Old state: {})", importItem.getFuncId(), importItem.getStatus());
				importItem.setStatus(ImportStatus.CREATED);
				break;
			case IMPORTED:
				// move the item as imported in the session only if import is completed
				logger.error("Item {} is in state IMPORTED whereas it is in the items to import list, database registration will be completed.", importItem.getFuncId());
				registerFunctionalIdent(importItem);
				registerMetadata(importItem);
				registerItemInDataset(importItem);
				importItem.setItemImported();
				break;
			case COMPLETED:
				// Should not be there...
				importItem.addError("The item was in the getItemsToImport list. Database consistency to be verified.");
				logger.error("The item {} was in the getItemsToImport list with 'COMPLETED' status. DB consistentcy to be checked", importItem.getTsuid());
				// move the item in the errors stack
				importItem.setItemInError();
				break;
			case CANCELLED:
			default:
				// move the item in the errors stack
				logger.error("Item {} with state {}, put into the items in error list.", importItem.getFuncId(), importItem.getStatus());
				importItem.setItemInError();
				break;
			}
		}
		
		logger.info("Cleaning pass on the items to import list done in {}", Duration.between(startDate, Instant.now()).toString());
	}

	/**
	 * Used to control the state of {@link ImportItemAnalyserThread}
	 */
	static enum ImportItemAnalyserState {
		/** Start state */
		INIT,
		/** Normal operation of the thread */
		RUNNING,
		/** In process of finishing */
		SHUTINGDOWN,
		/** Used to loop one more time after running is set to false */
		LASTPASS,
		/** End state */
		COMPLETED
	}
    // Review#147170 peut etre trop complexe ? ... 
	// Review#147170 pourquoi ne pas   - insérer un etat ImportStatus.RUNNING_REGISTER entre IMPORTED et COMPLETED:
	// Review#147170                   - ... et completer une seule machinea etat: celle du ImportSessionIngester:run()
	// Review#147170                   - ... et completer le ImportTaskFactory: createTask retourne ImportTask ou bien RegisterTask - un Callable par etat ...
	/**
	 * This inner class is designed to be run at start of the {@link ImportSessionIngester#run()} with goal to unstack the stack of {@link ImportItem} by running a loop on the {@link ImportSessionIngester#submitedTasks submitedTasks}<br>
	 * The main concern is on the status {@link ImportStatus#IMPORTED IMPORTED} of the {@link ImportItem} to :
	 * <ul>
	 *   <li>Call {@link ImportItem#setItemImported() setItemImported()} that removes that item from the list of items to ingest in the session
	 *   <li>Register the FunctionalIdentifier with {@link ImportItemAnalyserThread#registerFunctionalIdent(ImportItem) registerFunctionalIdent()}
	 *   <li>Register the other metadata/tags with {@link ImportItemAnalyserThread#registerMetadata(ImportItem) registerMetadata()}
	 *   <li>Register the TSUID in the Dataset, using a trick to defer that registration in a batch.<br>
	 *   See {@link ImportItemAnalyserThread#perpareToRegisterInDataset(ImportItem) perpareToRegisterInDataset} and {@link ImportItemAnalyserThread#registerTsuidsInDataset() registerTsuidsInDataset}
	 * </ul> 
	 * 
	 * <p><u>Note:</u> That pattern of deferred database update for the TSUID in Dataset could be generalized for all the database information, i.e. FunctionalIdentifier and Metadata.
	 */
	public class ImportItemAnalyserThread implements Runnable {

		/** State of the thread */
		private ImportItemAnalyserState state = ImportItemAnalyserState.INIT;
		
		/**
		 * Test with regard to this.state if if the loop should continue running
		 * @return true if the loop should continue running
		 */
		private boolean isRunning() {
			return state == ImportItemAnalyserState.RUNNING
					|| state == ImportItemAnalyserState.SHUTINGDOWN
					|| state == ImportItemAnalyserState.LASTPASS;
		}

		@Override
		public void run() {

			if (state == ImportItemAnalyserState.SHUTINGDOWN) {
				// case when stop() call was raised before Thread.start() has launched the current run() method
				logger.debug("Stopped before any run");
				state = ImportItemAnalyserState.COMPLETED;
				return;
			}
			
			// Start the loop in running state.
			state = ImportItemAnalyserState.RUNNING;
			
			while (isRunning()) {

				// Since we modify the list of results, we have to work in sync to avoid concurrency caveats
				synchronized (submitedTasks) {
					
					// unstack loop
					Iterator<Future<ImportItem>> iterator = submitedTasks.iterator();
					while (iterator.hasNext()) {
						Future<ImportItem> future = (Future<ImportItem>) iterator.next();
						
						if (!future.isDone()) {
							// skip rest of the unstack loop to iterate next.
							continue;
						}

						try {
							// Future<>.get() : should be immediate as we have only done() tasks
							ImportItem importItem = future.get();
							
							processImportItem(importItem);
							
						} catch (InterruptedException | ExecutionException e) {
							// We need to catch the exceptions and do nothing because we are in a Thread and it should terminate failsafe.
							logger.debug("Message: {}, cause: {}, {}", e.getMessage(), e.getCause(), e);
						}
						finally {
							// finally removes the current ImportResult out of the stack.
							iterator.remove();
						}
					}
				}
				
				// State machine control !
				switch (state) {
					case LASTPASS:
						// Were've just run the last iteration.
						// TODO If a timeout is implemented for the SHUTINGDOWN test, make some final record and check before running out the loop
						state = ImportItemAnalyserState.COMPLETED;
						break;
					case SHUTINGDOWN:
						// TODO implement test and timeout to check task that are not finished and reloop while tasks or timeout. 
						if (submitedTasks.isEmpty()) {
							// let an ultimate chance for tasks to finish
							state = ImportItemAnalyserState.LASTPASS;
							logger.trace("Last pass in the loop"); 
						}
						// Not breaking here is intentional to reach the sleep()
					case RUNNING:
						if (state != ImportItemAnalyserState.SHUTINGDOWN 
							&& (session.getStatus() == ImportStatus.COMPLETED
								|| session.getStatus() == ImportStatus.CANCELLED
								|| session.getStatus() == ImportStatus.ERROR)) {
							// Shutdown the thread if the session has been stopped
							state = ImportItemAnalyserState.SHUTINGDOWN;
						}
						// Not breaking here is intentional to reach the sleep()
					default: // continue to loop
						try {
							// Wait a moment before looping again.
							Thread.sleep(1000);
						} 
						catch (InterruptedException ie) {
							// We need to catch the InterruptedException and do nothing because we are in a Thread and it should terminate failsafe.
							logger.warn("Interrupted while waiting", ie);
						}
				}
			}
			logger.info("Finished analyzing sent tasks for session {} on dataset {}", session.getId(), session.getDataset()); 
			logger.debug("submitedTasks.size={}", submitedTasks.size()); 
		}

		
		/**
		 * Shutdown the thread by ending the loop with a last run.
		 */
		public void stop() {
			// used in the run() loop of ImportSessionIngester
			state = ImportItemAnalyserState.SHUTINGDOWN;
		}

		/**
		 * @param importItem
		 */
		private void processImportItem(ImportItem importItem) {
			// Check the import item status and move item from toImport list 
			// to one of the completed or erroneous list
			switch (importItem.getStatus()) {
			case CREATED:
				// A task has to be created via ImportItemTaskFactory 
			case ANALYSED:
				// The task has been created and will be run in a moment
			case RUNNING:
				// The import task is running
			case COMPLETED:
				// Finished, nothing to do
				break;
			case IMPORTED:
				// move the item as imported in the session only if import is completed
				registerFunctionalIdent(importItem);
				registerMetadata(importItem);
				registerItemInDataset(importItem);
				importItem.setItemImported();
				break;
			case ERROR:
			case CANCELLED:
			default:
				// move the item in the errors stack
				importItem.setItemInError();
				break;
			}
			
			// Update stats
			session.getStats().updateStats(importItem);
		}

	} // End class ImportItemAnalyserThread

	
	/**
	 * Register the current item (representing the time serie) into the dataset.<br>
	 */
	private void registerItemInDataset(ImportItem item) {
		
		try {
			// update the list of tsuid for the dataset
			DataSetFacade datasetService = process.getDatasetService();
			datasetService.updateInAppendMode(item.getTsuid(), item.getSession().getDataset());
			
			// Set final status on the item
			item.setStatus(ImportStatus.COMPLETED);
		} 
		catch (IkatsDaoException e) {
			session.addError(e.toString());
			session.addError("Exception " + e.getClass().getName() + " | Message: " + e.getMessage());
			if (! logger.isDebugEnabled()) {
				logger.error(e.getMessage());
				logger.error("Exception {} | Message: {}", e.getClass().getName(), e.getMessage());
			} else {
				logger.debug(e.getMessage(), e);
			}
			
			session.setStatus(ImportStatus.ERROR);
		}
		finally {
			// clear the list for next batch of tsuids.
			tsuidToRegister.clear();
		}
	}
	
	/**
	 * Register the Functional Identifier of the Ikats TS.
	 * 
	 * @param importItem the item for which the {@link ImportItem#getFuncId() FunctionalIdentifier} have to be registered.
	 */
	private void registerFunctionalIdent(ImportItem importItem) {
		
		try {
			// try to create the FID in the database.
			metaDataFacade.persistFunctionalIdentifier(importItem.getTsuid(), importItem.getFuncId());
		} 
		catch (IkatsDaoConflictException idce) {
			
			FunctionalIdentifier existingFID = null;
			try {
				existingFID = metaDataFacade.getFunctionalIdentifierByFuncId(importItem.getFuncId());
			} catch (IkatsDaoException e) {
				logger.warn("Exception while accessing the FunctionalIdentifier {}; Exception: {}", importItem.getFuncId(), e.toString(), e);
			}

			// Test if the FID is found
			if (existingFID != null) {
				// Ok check whether it has the same TSUID
				if (existingFID.getTsuid().equals(importItem.getTsuid())) {
					// Do nothing : there is an FID identified with the same TSUID : ok !
					logger.debug("DB complained for an already registered FID with the current FID. We do not have to do anything. (FuncId: {}, Tsuid: {})",
							importItem.getFuncId(), importItem.getTsuid());
				} 
				else {
					// yes, and it is not the same... -> ERROR
					logger.error("The FuncId {} is already registered for TSUID '{}' but the current item TSUID '{}'. Item to import marked in error state.",
							existingFID.getFuncId(), importItem.getTsuid());
					
					importItem.setStatus(ImportStatus.ERROR);
					importItem.addError("Existing different TSUID '" + existingFID.getTsuid() + "' in the database for the current item");
					session.setItemInError(importItem);
				}
			} 
			else {
				try {
                    // Test whether database hold a FuncId with the same TSUID
                    FunctionalIdentifier existingTSUID = metaDataFacade.getFunctionalIdentifierByTsuid(importItem.getTsuid());
                    if (existingTSUID != null && ! existingTSUID.getFuncId().equals(importItem.getFuncId())) {
                    	// and it is not the same...
                    	logger.warn("The TSUID {} is already registered with Functional Identifier '{}' but the current calculated is '{}'. Keeping the old one.",
                    			importItem.getTsuid(), existingTSUID.getFuncId(), importItem.getFuncId());
                    	importItem.addError("Existing funcId '" + existingTSUID.getFuncId() + "' for the current tsuid. The new calculated FuncId is overriden. Was '" + importItem.getFuncId() + "'" );
                    	importItem.setFuncId(existingTSUID.getFuncId());
                    } 
                    else {
                    	// Do nothing
                        logger.trace ("A pair TSUID/FuncId was already registered for the values {}/{}", importItem.getTsuid(), importItem.getFuncId());
                    }
                }
                catch (IkatsDaoException e) {
                    // Hazardous error
                    logger.error("Database error when checking FuncId by TSUID", e);
                }
			}
		}
		catch (IkatsDaoException e) {
			// An error occured during persist of the functional identifier
			String message = "Can't persist functional identifier '" +  importItem.getFuncId() + "' for tsuid " + importItem.getTsuid() + " ; item=" + importItem;
			importItem.addError(message);
			importItem.addError(e.getMessage());
			if (! logger.isDebugEnabled()) {
				logger.error(message);
				logger.error(e.getMessage());
			} else {
				logger.debug(message, e);
			}
			
			importItem.setStatus(ImportStatus.ERROR);
			session.setItemInError(importItem);
		}
	}
	
	/**
	 * Register a metadata for each tag of the item.
	 * @param importItem the item for which the {@link ImportItem#getTags()} have to be registered as metadata
	 */
	private void registerMetadata(ImportItem importItem) {
		
		ArrayList<MetaData> metadataList = new ArrayList<MetaData>();
		
		// set the metric as metadata
		MetaData mdMetric = new MetaData();
		mdMetric.setName("metric");
		mdMetric.setDType(MetaType.string);
		mdMetric.setValue(importItem.getMetric());
		mdMetric.setTsuid(importItem.getTsuid());
		metadataList.add(mdMetric);
		
		// set the start and end dates as metadata
		MetaData mdStartDate = new MetaData();
		mdStartDate.setName("ikats_start_date");
		mdStartDate.setDType(MetaType.date);
		mdStartDate.setValue(Long.toString(importItem.getStartDate().toEpochMilli()));
		mdStartDate.setTsuid(importItem.getTsuid());
		metadataList.add(mdStartDate);
		MetaData mdEndDate = new MetaData();
		mdEndDate.setName("ikats_end_date");
		mdEndDate.setDType(MetaType.date);
		mdEndDate.setValue(Long.toString(importItem.getEndDate().toEpochMilli()));
		mdEndDate.setTsuid(importItem.getTsuid());
		metadataList.add(mdEndDate);
		
		// set the number of points
		MetaData mdNbPoints = new MetaData();
		mdNbPoints.setName("qual_nb_points");
		mdNbPoints.setDType(MetaType.number);
		mdNbPoints.setValue(Long.toString(importItem.getNumberOfSuccess()));
		mdNbPoints.setTsuid(importItem.getTsuid());
		metadataList.add(mdNbPoints);
		
		
		// Set the dataset tags as metadata
		importItem.getTags().entrySet().forEach( tag -> {
			MetaData mdTag = new MetaData();
			mdTag.setName(tag.getKey());
			mdTag.setDType(MetaType.string);
			mdTag.setValue(tag.getValue());
			mdTag.setTsuid(importItem.getTsuid());
			metadataList.add(mdTag);
		} );
		
		try {
			// Save all the metadata with update option.
			metaDataFacade.persist(metadataList, true);
		}
		catch (IkatsDaoException e) {
			// An error occured during persist or update
			String message = "Can't persist metadata for tsuid " + importItem.getTsuid() + " ; item=" + importItem.getFuncId();
			importItem.addError(message);
			importItem.addError(e.getMessage());
			if (!logger.isDebugEnabled()) {
				logger.error(message);
				logger.error(e.getMessage());
			} else {
				logger.debug(message, e);
			}
			
			// mark the item not fully "ingested"
			importItem.setStatus(ImportStatus.ERROR);
			session.setItemInError(importItem);
		}
	}
	
}  // End ImportSessionIngester
