package fr.cs.ikats.ingestion.process;

import java.util.concurrent.Callable;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;

/**
 * Factory which create a task that do not import anything and mark the {@link ImportItem} as {@link ImportStatus#CANCELLED CANCELLED}
 * @author ftoral
 */
@Stateless
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
	
	/**
	 * 
	 * @author ftoral
	 */
	class ImportTask implements Callable<ImportItem> {
		
		private ImportItem importItem;

		public ImportTask(ImportItem importItem) {
			this.importItem = importItem;
		}

		@Override
		public ImportItem call() throws Exception {
			logger.debug("DefaultImportNothingTaskFactory for {}", importItem);
			// Mark import item as cancelled
			this.importItem.setStatus(ImportStatus.CANCELLED);
			return this.importItem;
		}
			
	}
}
