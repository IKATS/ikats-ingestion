package fr.cs.ikats.ingestion.process;

import java.util.ArrayList;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.common.dao.exception.IkatsDaoException;
import fr.cs.ikats.common.dao.exception.IkatsDaoMissingResource;
import fr.cs.ikats.ingestion.exception.IngestionException;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ts.dataset.DataSetFacade;
import fr.cs.ikats.ts.dataset.model.DataSet;
import fr.cs.ikats.util.concurrent.ExecutorPoolManager;

/**
 *
 * That class represent a thread that manage the full ingestion of a single {@link ImportSession} 
 * @author ftoral
 *
 */
public class IngestionProcess implements Runnable {

	private ManagedThreadFactory threadFactory;
	private ImportSession session;
	private ExecutorPoolManager executorPoolManager;
	
	private Logger logger = LoggerFactory.getLogger(IngestionProcess.class);
	private DataSetFacade dataSetFacade;

	/**
	 * Creates the thread for a {@link ImportSession session}, with a specific threadFactory that is provided by the J2EE container and an preconfigured {@link ExecutorPoolManager} to submit import tasks. 
	 * @param session the description of the ingestion session
	 * @param threadFactory the container thread facility 
	 * @param executorPoolManager a specific thread queue for import tasks
	 */
	public IngestionProcess(ImportSession session, ManagedThreadFactory threadFactory, ExecutorPoolManager executorPoolManager) {
		this.threadFactory = threadFactory;
		this.session = session;
		this.executorPoolManager = executorPoolManager;
	}
	
	/**
	 * @return the executorPoolManager
	 */
	public ExecutorPoolManager getExecutorPool() {
		return executorPoolManager;
	}
	
	/**
	 * @return the dataset facade service
	 */
	public DataSetFacade getDatasetService() {
		
		if (dataSetFacade == null) {
			dataSetFacade = new DataSetFacade();
		}
		
		return dataSetFacade;
	}

	/**
	 * <p>That thread is designed to loop until an {@link ImportSession} is declared {@link ImportStatus#IMPORTED IMPORTED}.<br>
	 * At each stage, of {@link ImportStatus}, it launches a new task, until IMPORTED.</p>
	 * 
	 * <p>Before that thread, when the {@link ImportSession} request is recieved by the application, the status is CREATED. Then the state machine runs.
	 * </p>
	 * 
	 * When the sessions is:
	 * <ul>
	 *   <li>CREATED, then a {@link ImportAnalyser} runs to analyse the dataset and the TS to ingest/import.<br>
	 *   When done, the state is upgraded to
	 *   <li>ANALYSED, then the dataset is registered ({@link IngestionProcess#registerDataset(ImportSession) registerDataset(ImportSession)}) without TSUIDS.<br>
	 *   When done, the state is upgraded to
	 *   <li>DATASET_REGISTERED, then a new thread launches an {@link ImportSessionIngester}.
	 *   <li>During ingestion, the session should be in state RUNNING, and the {@link ImportSessionIngester} thread is tested to be in a live state.<br>
	 *   A completion, the state is upgraded to 
	 *   <li>COMPLETED 
	 * </ul>
	 */
	public void run() {
		
		// Implement life cycle :
		
		// 1- Analyse import to do : prepare ImportSession object
		// 2- Create the dataset
		// 3- Launch first batch import
		//    For each item : if OpenTSDB import OK, add it to the dataset, create the metadata 
		// 4- Analyse first batch import
		// 5- Loop to 2 until each item is imported if previous analyse permits it.
		
		// manage only one ImportSession
	    // Review#147170 expliquer l'interet d'un sous-thread runner unique de ce thread IngestionProcess ... obligé ?
	    // Review#147170 on pourrait utiliser directement Runnable::run() sinon (donc peu de modifs)
		Thread runner = null;
		while(session.getStatus() != ImportStatus.COMPLETED
				&& session.getStatus() != ImportStatus.CANCELLED
				&& session.getStatus() != ImportStatus.ERROR) {
			
			switch (session.getStatus()) {
				case CREATED:
					runner = threadFactory.newThread(new ImportAnalyser(this.session));
					runner.start();
					break;
				case ANALYSED:
					logger.info("Import session analysed: Dataset={}, Nb Items to import={}", session.getDataset(), session.getItemsToImport().size());
					try {
						if (session.getItemsToImport() != null && session.getItemsToImport().size() > 0) {
							// Register the dataset if there is something to import.
							registerDataset(session);
							session.setStatus(ImportStatus.DATASET_REGISTERED);
						} else {
							// Nothing to do : cancel the session
							session.addError("Nothing to import");
							session.setStatus(ImportStatus.CANCELLED);
						}
					} catch (IngestionException ie) {
						String message = "Can't persist dataset '" + session.getDataset() + "' for import session " + session.getId();
						session.addError(message);
						session.addError(ie.getMessage());
						logger.error(message, ie);
						session.setStatus(ImportStatus.CANCELLED);
					}
					break;
				case DATASET_REGISTERED:
					logger.info("Datset {} registered in IKATS", session.getDataset());
					try {
						runner = threadFactory.newThread(new ImportSessionIngester(this, this.session));
						this.session.setStatus(ImportStatus.RUNNING);
						runner.start();
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException cnfe) {
						// Cancel the session
						session.addError("Import session cancelled due to NamingException (" + cnfe.getMessage() + ") while registerering the dataset: " + session.getDataset());
						logger.error("Import session cancelled for dataset " + session.getDataset() + "", cnfe);
						this.session.setStatus(ImportStatus.CANCELLED);
					}
					break;
				case RUNNING:
					// Do nothing while ImportSession is running
					// check only that everything is fine
					if (runner == null || ! runner.isAlive() || runner.isInterrupted()) {
						logger.error("Session {} is in illegal state while ingestion thread is in error. Setting session to cancelled", session.getId());
						session.setStatus(ImportStatus.CANCELLED);
					}
					break;
				case COMPLETED:
					// finished.
					break;
				default:
				    // Review#147170 pourqoi pas gerer l'exception maintenant ?
					// TODO manage an exception here when implementation will be full
					//break;
					// For instance, set import cancelled :
					session.setStatus(ImportStatus.CANCELLED);
					continue;
			}
			// Review#147170 si on garde le type Thread pour runner:
			// Review#147170   pourquoi eloigner ce bout de code des start() ? ...
			// Review#147170   j'aurais fait une methode privee joinRunner(...), et appelé joinRunner(runner) avant les break ...
			// Review#147170 si on abandonne le type Thread pour runner => Runnable: remplacer start() par run() ... et supprimer ce code de join
			if (runner != null && runner.isAlive()) {
				try {
					// Lock the current thread until the new nested process thread is finished
					runner.join();
				} catch (InterruptedException ie) {
					// TODO manage error ?
					logger.warn("Interrupted while waiting", ie);
				}
			}
			// end loop
		}
		
	}
	
	/**
	 * Register the dataset from the session
	 * @param session
	 * @throws IngestionException 
	 */
	private void registerDataset(ImportSession session) throws IngestionException {
		
		try {
			dataSetFacade = new DataSetFacade();
			DataSet dataSet = dataSetFacade.getDataSet(session.getDataset());
			logger.warn("Dataset {} already registered", dataSet.getName());
		}
	
		// FIXME : to be changed in the DAO, when no dataset with that name is found, the DAO raises an Exception. It should instead return null.
		// Review#147170 attention effet de bord FIXME: IkatsDaoMissingRessource est associée à un handler d'erreur pour les codes
		// Review#147170 de retour Rest: si on retourne null il faudra corriger les services Rest impactés ...
		// Review#147170 sinon: faire un service hasDataset(...) sur la facade ? 
		catch (IkatsDaoMissingResource e) {
			try {
				// register only if the dataset doesn't exists in database
				dataSetFacade.persistDataSet(session.getDataset(), session.getDescription(), new ArrayList<String>(0));
			}
			catch (IkatsDaoException ide) {
				throw new IngestionException(ide);
			}
		}
		catch (IkatsDaoException e) {
			throw new IngestionException(e);
		}
		
	}

}
