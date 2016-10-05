package fr.cs.ikats.ingestion.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class ImportSessionIngester implements Runnable {

	/** The session on which to work */
	private ImportSession session;

	/** References the caller process */
	private IngestionProcess process;

	/** Factory that creates task for low level import (currently OpenTSDB) */
	private ImportItemTaskFactory importItemTaskFactory;

	/** Synchronized list of ({@link Future}) tasks */
	private List<Future<ImportItem>> submitedTasks = Collections
			.synchronizedList(new ArrayList<Future<ImportItem>>());
	
	private Logger logger = LoggerFactory.getLogger(ImportSessionIngester.class);

	/**
	 * 
	 * @param ingestionProcess
	 * @param session
	 * @throws NamingException
	 */
	public ImportSessionIngester(IngestionProcess ingestionProcess, ImportSession session) throws NamingException {
		this.process = ingestionProcess;
		this.session = session;

		Context ctx = new InitialContext();
		// Get OpenTSDB importer
		// TODO for future implementations : find another way to lookup corresponding the ImportItemTaskFactory
		// i.e. : 
		//  - type attribute in ImportSession
		//  - ...
		importItemTaskFactory = (ImportItemTaskFactory) ctx.lookup("java:global/ikats-ingestion/OpenTsdbImportTaskFactory");
		logger.info("ImportItemTaskFactory injected as {}", importItemTaskFactory.getClass().getName());
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

		// Notify parent that process is finished
		process.continueProcess();
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
	class ImportItemAnalyserThread implements Runnable {

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

			// Start the loop in running state.
			state = ImportItemAnalyserState.RUNNING;
			
			while (isRunning()) {
				
				// Since we modify the list of results, we have to work in sync to avoid concurrency caveats
				synchronized (submitedTasks) {
					
					// unstack loop
					ListIterator<Future<ImportItem>> iterator = submitedTasks.listIterator();
					while (iterator.hasNext()) {
						Future<ImportItem> future = (Future<ImportItem>) iterator.next();
						if (!future.isDone()) {
							logger.debug("Import task not finished and will be analysed in next loop: {}", future);
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
									// move the item as imported in the session only if import is completed
									importItem.setItemImported();
									break;
								case CANCELLED:
								case STOPPED:
								default:
									// move the item in the errors stack
									importItem.setItemInError();
									break;
							}
							
							// finally removes the current ImportResult out of the stack.
							iterator.remove();
							
						} catch (InterruptedException | ExecutionException e) {
							// FIXME
							logger.debug("Message: {}, cause: {}, {}", e.getMessage(), e.getCause(), e);
						}
					}
				}
				
				// State machine control !
				switch (state) {
				case LASTPASS:
					// Were've just run the last itreation :
					state = ImportItemAnalyserState.COMPLETED;
				case SHUTINGDOWN:
					// FIXME : implement test and timeout to check task that are not finished and reloop while tasks or timeout. 
					if (submitedTasks.size() == 0) {
						state = ImportItemAnalyserState.COMPLETED;
					}
					break;
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
			
			
			
		}

		/**
		 * Shutdown the thread by ending the loop with a last run.
		 */
		public void stop() {
			// used in the run() loop
			state = ImportItemAnalyserState.SHUTINGDOWN;
		}
	}

}
