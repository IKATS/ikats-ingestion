package fr.cs.ikats.ingestion.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.naming.NamingException;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.common.dao.exception.IkatsDaoException;
import fr.cs.ikats.ingestion.Configuration;
import fr.cs.ikats.ingestion.IngestionConfig;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.metadata.MetaDataFacade;
import fr.cs.ikats.ts.dataset.DataSetFacade;

@Stateless
public class ImportSessionIngester implements Runnable {

	private static int REGISTER_TSUID_DATASET_BATCH_SIZE = 0;
	
	/** The session on which to work */
	private ImportSession session;

	/** References the caller process */
	private IngestionProcess process;

	/** Factory that creates task for low level import (currently OpenTSDB) */
	private ImportItemTaskFactory importItemTaskFactory;

	/** Synchronized list of ({@link Future}) tasks */
	private List<Future<ImportItem>> submitedTasks = Collections
			.synchronizedList(new ArrayList<Future<ImportItem>>());
	
	private MetaDataFacade metaDataFacade;

	private String funcIdPattern;
	
	private Logger logger = LoggerFactory.getLogger(ImportSessionIngester.class);
	
	@SuppressWarnings("unused")
	private ImportSessionIngester() {
		
	}
	
	/**
	 * 
	 * @param ingestionProcess
	 * @param session
	 * @throws NamingException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ImportSessionIngester(IngestionProcess ingestionProcess, ImportSession session) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		this.process = ingestionProcess;
		this.session = session;
		this.funcIdPattern = session.getFuncIdPattern();

		// Get the factory to import session items. The default test implementation is used if none found.
		String taskFactoryFQN = session.getImporter();
		if (taskFactoryFQN == null) {
			taskFactoryFQN = (String) Configuration.getInstance().getProperty(IngestionConfig.IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY);
		}
//		String taskFactoryName = taskFactoryFQN.substring(taskFactoryFQN.lastIndexOf('.') + 1);
		
//		Context ctx = new InitialContext();
//		importItemTaskFactory = (ImportItemTaskFactory) ctx.lookup("java:global/ikats-ingestion/" + taskFactoryName);
		
//		Class<?> importItemTaskFactoryClazz = Class.forName(taskFactoryFQN, false, this.getClass().getClassLoader()); 
		Class<?> importItemTaskFactoryClazz = getClass().getClassLoader().loadClass(taskFactoryFQN); 
		importItemTaskFactory = (ImportItemTaskFactory) importItemTaskFactoryClazz.newInstance();
		logger.info("ImportItemTaskFactory injected as {}", importItemTaskFactory.getClass().getName());
		
		// Instance the facade for metadata creation 
		metaDataFacade = new MetaDataFacade();

		REGISTER_TSUID_DATASET_BATCH_SIZE = (int) Configuration.getInstance().getInt(IngestionConfig.IKATS_INGESTER_TSUIDTODATASET_BATCH);
	}

	@Override
	public void run() {

		// Launch import results analyser thread
		ImportItemAnalyserThread importItemAnalyserThread = new ImportItemAnalyserThread();
		Thread thread = new Thread(importItemAnalyserThread);
		thread.start();

		// Launch the import loop
		// The state is controlled on the 
		while (session.getStatus() == ImportStatus.RUNNING
				&& submitedTasks.size() < session.getItemsToImport().size()) {

			// at each loop get the last list of items to import
			ListIterator<ImportItem> listIterator = session.getItemsToImport().listIterator();
			while (listIterator.hasNext()) {
				
				ImportItem importItem = (ImportItem) listIterator.next();

				// for each one create and submit and import task
				if (importItem.getStatus() == ImportStatus.CREATED) { 
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
				logger.warn("Interrupted while waiting", ie);
			}
		}

		// launch stop command to the result analysis thread and wait for it to
		// finish
		importItemAnalyserThread.stop();
		try {
			thread.join();
		} catch (InterruptedException ie) {
			logger.error("Interrupted while waiting importItemAnalyserThread to finish", ie);
		}

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

	/**
	 * This inner class is designed to be run at each loop of the
	 * {@link ImportSessionIngester.run()}<br>
	 * The goal is to unstack the stack of {@link Future<ImportResult>}
	 */
	public class ImportItemAnalyserThread implements Runnable {

		/** State of the thread */
		private ImportItemAnalyserState state = ImportItemAnalyserState.INIT;
		private List<String> tsuidToRegister = new ArrayList<String>();

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

			// Start the loop in running state.
			state = ImportItemAnalyserState.RUNNING;
			
			while (isRunning()) {

				// Since we modify the list of results, we have to work in sync to avoid concurrency caveats
				synchronized (submitedTasks) {
					
					// unstack loop
					Iterator<Future<ImportItem>> iterator = submitedTasks.iterator();
					int index = 0;
					while (iterator.hasNext()) {
						Future<ImportItem> future = (Future<ImportItem>) iterator.next();
						index++;
						
						if (!future.isDone()) {
							// skip rest of the unstack loop to iterate next.
							continue;
						}

						try {
							// Future<>.get() : should be immediate as we have only done() tasks
							ImportItem importItem = future.get();
							
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
								case STOPPED:
									break;
								case IMPORTED:
									// move the item as imported in the session only if import is completed
									importItem.setItemImported();
									registerFunctionalIdent(importItem);
									registerMetadata(importItem);
									perpareToRegisterInDataset(importItem);
									break;
								case CANCELLED:
								default:
									// move the item in the errors stack
									importItem.setItemInError();
									break;
							}
							
							// finally removes the current ImportResult out of the stack.
							iterator.remove();
							
							// register a batch of tsuid in the dataset
							if (index % REGISTER_TSUID_DATASET_BATCH_SIZE == 0 || iterator.hasNext() == false) {
								registerTsuidsInDataset();
							}
							
						} catch (InterruptedException | ExecutionException e) {
							// FIXME
							logger.debug("Message: {}, cause: {}, {}", e.getMessage(), e.getCause(), e);
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
						if (submitedTasks.size() == 0) {
							// let an ultimate chance for tasks to finish
							state = ImportItemAnalyserState.LASTPASS;
							logger.debug("Last pass in the loop"); 
						}
					case RUNNING:
					default: // continue to loop
						try {
							// Wait a moment before looping again.
							Thread.sleep(1000);
						} catch (InterruptedException ie) {
							// TODO manage error ?
							logger.warn("Interrupted while waiting", ie);
						}
				}
			}
			logger.info("Finished running submitedTasks.size={}", submitedTasks.size()); 
		}
		
		/**
		 * Shutdown the thread by ending the loop with a last run.
		 */
		public void stop() {
			// used in the run() loop
			state = ImportItemAnalyserState.SHUTINGDOWN;
		}
		
		/**
		 * Record the item for future linking in the dataset by {@link ImportItemAnalyserThread#registerTsuidsInDataset() registerTsuidsInDataset}
		 * @param importItem
		 */
		private void perpareToRegisterInDataset(ImportItem importItem) {
			if (tsuidToRegister == null) {
				tsuidToRegister = new ArrayList<String>();
			}
			
			// add the tsuid to the batch
			tsuidToRegister.add(importItem.getTsuid());
		}
		
		/**
		 * Register the current list of IKATS TS into the dataset 
		 */
		private void registerTsuidsInDataset() {
			
			try {
				// update the list of tsuid for the dataset
				DataSetFacade datasetService = process.getDatasetService();
				datasetService.updateInAppendMode(session.getDataset(), null, tsuidToRegister);
			} catch (IkatsDaoException | NullPointerException e) {
				String message = "Can't register a list of tsuids in dataset '" + session.getDataset() + "'";
				session.addError(message);
				session.addError("Exception " + e.getClass().getName() + " | Message: " + e.getMessage());
				if (! logger.isDebugEnabled()) {
					logger.error(message);
					logger.error("Exception {} | Message: {}", e.getClass().getName(), e.getMessage());
				} else {
					logger.debug(message, e);
				}
				
				session.setStatus(ImportStatus.STOPPED);
			}
			finally {
				// clear the list for next batch of tsuids.
				tsuidToRegister.clear();
			}
		}
		
		/**
		 * Register the Functional Identifier of the Ikats TS.
		 * @param importItem
		 */
		private void registerFunctionalIdent(ImportItem importItem) {
			
			// format the functional identifier from pattern and tags
			Map<String, String> valuesMap = new HashMap<String, String>();
			valuesMap.putAll(importItem.getTags());
			valuesMap.put("metric", importItem.getMetric());
			StrSubstitutor sub = new StrSubstitutor(valuesMap);
			String funcId = sub.replace(funcIdPattern);
			 
			try {
				importItem.setFuncId(funcId);
				metaDataFacade.persistFunctionalIdentifier(importItem.getTsuid(), funcId);
			} catch (IkatsDaoException e) {
				String message = "Can't persist functional identifier '" +  importItem.getFuncId() + "' for tsuid " + importItem.getTsuid() + " ; item=" + importItem;
				importItem.addError(message);
				importItem.addError(e.getMessage());
				if (! logger.isDebugEnabled()) {
					logger.error(message);
					logger.error(e.getMessage());
				} else {
					logger.debug(message, e);
				}
				
				importItem.setStatus(ImportStatus.STOPPED);
			}
		}
		
		/**
		 * Register a metadata for each tag of the time serie.
		 * @param importItem
		 */
		private void registerMetadata(ImportItem importItem) {

			Set<Entry<String, String>> tagsKV = importItem.getTags().entrySet();
			for (Entry<String, String> tag : tagsKV) {
				try {
					metaDataFacade.persistMetaData(importItem.getTsuid(), tag.getKey(), tag.getValue());
				} catch (IkatsDaoException e) {
					String message = "Can't persist metadata (k/v) '" + tag.getKey() + "/" + tag.getValue() + "' for tsuid " + importItem.getTsuid() + " ; item=" + importItem;
					importItem.addError(message);
					importItem.addError(e.getMessage());
					if (! logger.isDebugEnabled()) {
						logger.error(message);
						logger.error(e.getMessage());
					} else {
						logger.debug(message, e);
					}

					
					importItem.setStatus(ImportStatus.STOPPED);
				}
			}
		}

	}

}
