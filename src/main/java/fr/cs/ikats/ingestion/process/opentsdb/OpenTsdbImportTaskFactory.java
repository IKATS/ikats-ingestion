package fr.cs.ikats.ingestion.process.opentsdb;

import java.time.Instant;
import java.util.concurrent.Callable;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportItemTaskFactory;

/**
 * Factory which creates OpenTSDB import task 
 * @author ftoral
 */
@Stateless
public class OpenTsdbImportTaskFactory implements ImportItemTaskFactory {

	private Logger logger = LoggerFactory.getLogger(OpenTsdbImportTaskFactory.class);
	
	public OpenTsdbImportTaskFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Callable<ImportItem> createTask(ImportItem item) {
		
		ImportTask task = new ImportTask(item);
		return task;
	}
	
	/**
	 * 
	 * @author ftoral
	 */
	class ImportTask implements Callable<ImportItem> {
		
		private ImportItem importItem;

		public ImportTask(ImportItem importItem) {
			this.importItem = importItem;
			this.importItem.setStatus(ImportStatus.ANALYSED);
		}

		@Override
		public ImportItem call() throws Exception {
			this.importItem.setStartDate(Instant.now());
			logger.info("Importing {}", importItem.getFile());
			importItem.setStatus(ImportStatus.RUNNING);
			Thread.sleep(5000);
			this.importItem.setEndDate(Instant.now());
			importItem.setStatus(ImportStatus.COMPLETED);
			return this.importItem;
		}
			
	}
}
