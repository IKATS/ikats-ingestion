package fr.cs.ikats.ingestion.process;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;

/**
 * Factory which create a task that do not import anything and mark the {@link ImportItem} as {@link ImportStatus#CANCELLED CANCELLED}
 * @author ftoral
 */
public class DefaultImportNothingTaskFactory implements ImportItemTaskFactory {

	private Logger logger = LoggerFactory.getLogger(DefaultImportNothingTaskFactory.class);
	
	public DefaultImportNothingTaskFactory() {
		// Do nothing
	}

	@Override
	public Callable<ImportItem> createTask(ImportItem item) {
		ImportTask task = new ImportTask(item);
		return task;
	}
	// Review#147170  qqs explications javadoc sur ImportTask 
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
			logger.debug("Process {} for {}", getClass().getName(), importItem);
			// Mark import item as cancelled
			this.importItem.addError("Processed by " + getClass().getName());
			this.importItem.setStatus(ImportStatus.CANCELLED);
			return this.importItem;
		}
			
	}
}
