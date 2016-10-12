package fr.cs.ikats.ingestion.process.opentsdb;

import java.time.Instant;
import java.util.concurrent.Callable;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportItemTaskFactory;
import fr.cs.ikats.util.configuration.ConfigProperties;
import fr.cs.ikats.util.configuration.IkatsConfiguration;

/**
 * Factory which creates OpenTSDB import task 
 * 
 * @author ftoral
 */
@Stateless
public class OpenTsdbImportTaskFactory implements ImportItemTaskFactory {

	private IkatsConfiguration<ConfigProps> config = new IkatsConfiguration<ConfigProps>(ConfigProps.class);

	private Logger logger = LoggerFactory.getLogger(OpenTsdbImportTaskFactory.class);

	private String openTsdbUrl;
	
	public OpenTsdbImportTaskFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Callable<ImportItem> createTask(ImportItem item) {
		ImportTask task = new ImportTask(item);
		openTsdbUrl = (String) config.getProperty(ConfigProps.OPENTSDB_IMPORT_URL);
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
			logger.info("OpenTSDB url: {}", openTsdbUrl);
			this.importItem.setStartDate(Instant.now());
			logger.info("Importing {}", importItem.getFile());
			importItem.setStatus(ImportStatus.RUNNING);
			Thread.sleep(5000);
			this.importItem.setEndDate(Instant.now());
			importItem.setStatus(ImportStatus.COMPLETED);
			return this.importItem;

		}
	}

	public enum ConfigProps implements ConfigProperties {

		OPENTSDB_IMPORT_URL("opentsdb.api.import");

		// Filename
		public final static String propertiesFile = "opentsdb.properties";

		private String propertyName;
		private String defaultValue;
		
		// Mandatory default constructor
		ConfigProps() {};

		ConfigProps(String propertyName, String defaultValue) {
			this.propertyName = propertyName;
			this.defaultValue = defaultValue;
		}
			
		ConfigProps(String propertyName) {
			this.propertyName = propertyName;
			this.defaultValue = null;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getPropertiesFilename() {
			return propertiesFile;
		}
	}
}
