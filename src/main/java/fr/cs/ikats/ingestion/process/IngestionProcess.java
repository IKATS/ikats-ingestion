package fr.cs.ikats.ingestion.process;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class IngestionProcess implements Runnable {

	private ManagedThreadFactory threadFactory;
	private ImportSession session;
	
	private Logger logger = LoggerFactory.getLogger(IngestionProcess.class);

	public IngestionProcess(ManagedThreadFactory threadFactory, ImportSession session) {
		this.threadFactory = threadFactory;
		this.session = session;
	}

	@Override
	public void run() {
		
		// Implement life cycle :
		
		// 1- Analyse import to do : prepare ImportSession object
		// 2- Create the dataset
		// 3- Launch first batch import
		//    For each item : if OpenTSDB import OK, add it to the dataset, create the metadata 
		// 4- Analyse first batch import
		// 5- Loop to 2 until each item is imported if previous analyse permits it.
		
		// manage only on ImportSession
		while(session.getStatus() != ImportStatus.COMPLETED
				|| session.getStatus() != ImportStatus.CANCELLED
				|| session.getStatus() != ImportStatus.STOPPED) {
			
			switch (session.getStatus()) {
			case CREATED:
				Thread analyser = threadFactory.newThread(new ImportAnalyser(this.session));
				analyser.start();
				break;
			case ANALYSED:
				logger.info("Import session analysed: Dataset={}, Nb Items to import={}", session.getDataset(), session.getItemsToImport().size());
				registerDataset(session.getDataset());
				break;
			case DATASET_REGISTERED:
				logger.info("Datset {} registered in IKATS", session.getDataset());
			case RUNNING:
			case COMPLETED:
			default:
				// TODO manage an exception here when implementation will be full
				//break;
				// For instance, set import cancelled :
				session.setStatus(ImportStatus.CANCELLED);
				continue;
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO manage error ?
			}
		}
		

	}

	private void registerDataset(String string) {
		// TODO Auto-generated method stub
		
	}

}
