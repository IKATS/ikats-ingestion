package fr.cs.ikats.ingestion.process.opentsdb;

import java.util.Date;
import java.util.concurrent.Callable;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportResult;
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
	public Callable<ImportResult> createTask(ImportItem item) {
		
		ImportTask task = new ImportTask(item);
		return task;
	}
	
	/**
	 * 
	 * @author ftoral
	 */
	class ImportTask implements Callable<ImportResult> {
		
		private ImportItem importItem;

		public ImportTask(ImportItem importItem) {
			this.importItem = importItem;
			this.importItem.setStatus(ImportStatus.ANALYSED);
		}

		@Override
		public ImportResult call() throws Exception {
			ImportResult result = new ImportResult();
			long now = new Date().getTime();
			result.setStartDate(now );
			result.setImportItem(importItem);
			logger.info("Importing {}", importItem.getFile());
			importItem.setStatus(ImportStatus.RUNNING);
			Thread.sleep(5000);
			importItem.setStatus(ImportStatus.COMPLETED);
			return result;
		}
		
	}
}
