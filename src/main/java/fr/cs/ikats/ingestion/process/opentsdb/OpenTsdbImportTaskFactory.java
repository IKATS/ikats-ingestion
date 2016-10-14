package fr.cs.ikats.ingestion.process.opentsdb;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import fr.cs.ikats.datamanager.DataManagerException;
import fr.cs.ikats.datamanager.client.RequestSender;
import fr.cs.ikats.datamanager.client.importer.IImportSerializer;
import fr.cs.ikats.datamanager.client.opentsdb.DataBaseClientManager;
import fr.cs.ikats.datamanager.client.opentsdb.IkatsWebClientException;
import fr.cs.ikats.datamanager.client.opentsdb.ImportResult;
import fr.cs.ikats.datamanager.client.opentsdb.ResponseParser;
import fr.cs.ikats.ingestion.exception.IngestionException;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportItemTaskFactory;
import fr.cs.ikats.ingestion.serializer.ImportSerializerFactory;
import fr.cs.ikats.util.configuration.ConfigProperties;
import fr.cs.ikats.util.configuration.IkatsConfiguration;

/**
 * Factory which creates OpenTSDB import task 
 * 
 * @author ftoral
 */
@Stateless
public class OpenTsdbImportTaskFactory implements ImportItemTaskFactory {

	@EJB
	private ImportSerializerFactory importSerializerFactory;
	
	private final static IkatsConfiguration<ConfigProps> config = new IkatsConfiguration<ConfigProps>(ConfigProps.class);
	
    private final static Pattern tsuidPattern = Pattern.compile(".*tsuids\":\\[\"(\\w*)\"\\].*");

    /** The OpenTSDB client manager instance */
    private final static DataBaseClientManager urlBuilder = new DataBaseClientManager();

    private final int IMPORT_NB_POINTS_BY_BATCH;
    
	private Logger logger = LoggerFactory.getLogger(OpenTsdbImportTaskFactory.class);

	public OpenTsdbImportTaskFactory() {
		IMPORT_NB_POINTS_BY_BATCH = (int) config.getProperty(ConfigProps.IMPORT_CHUNK_SIZE);
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
		public ImportItem call() {

			try {
				IImportSerializer jsonizer = importSerializerFactory.getSerializer(this.importItem);

				// Set import running for that item.
				importItem.setStatus(ImportStatus.RUNNING);
				
				// 1- Send the TS
				sendItemInChunks(jsonizer);
				
				// 2- Get the resulting TSUID
		        String tsuid = getTSUID(importItem.getMetric(), jsonizer.getDates()[0], importItem.getTags());
		        importItem.setTsuid(tsuid);
		        if (tsuid == null || tsuid.isEmpty()) {
		        	throw new IngestionException("Could not get OpenTSDB tsuid for item: " + importItem);
		        } 
		        
		        importItem.setStatus(ImportStatus.IMPORTED);
		        
		        // 3- Provide ImportItem with imported key values
				importItem.setStartDate(Instant.ofEpochMilli(jsonizer.getDates()[0]));
				importItem.setEndDate(Instant.ofEpochMilli(jsonizer.getDates()[1]));

			} catch (IngestionException | IOException | DataManagerException | IkatsWebClientException e) {
				FormattingTuple arrayFormat = MessageFormatter.format("Error while processing item for file {} ",
						importItem.getFile().toString());
				logger.error(arrayFormat.getMessage(), e);
				importItem.addError(e.getMessage());
				importItem.setStatus(ImportStatus.CANCELLED);
			}

			// the import item was provided with all its new properties
			return this.importItem;

		}

		/**
		 * @param jsonizer
		 * @throws IOException
		 * @throws DataManagerException
		 */
		private void sendItemInChunks(IImportSerializer jsonizer) throws IOException, DataManagerException {
			// Create an aggregated ImportResult for the entire item
			int chunkIndex = 0;

			// loop to submit import request for each TS chunk
			while (jsonizer.hasNext()) {
				chunkIndex ++;
				String json = jsonizer.next(IMPORT_NB_POINTS_BY_BATCH);
				try {
					if (json != null && !json.isEmpty()) {
						String url = (String) config.getProperty(ConfigProps.OPENTSDB_IMPORT_URL);
						logger.debug("Sending request to {}" + url);
						Response response = RequestSender.sendPUTJsonRequest(url, json);
						ImportResult result = ResponseParser.parseImportResponse(response, response.getStatus());
						logger.debug("Import task finished with result: " + result);
						
						// Aggregate the result of this chunk into the item result
						importItem.addNumberOfSuccess(result.getNumberOfSuccess());
						importItem.addNumberOfFailed(result.getNumberOfFailed());
						for (Entry<String, String> error : result.getErrors().entrySet()) {
							String details = "[chunk #" + chunkIndex + "] " + error.getValue();
							importItem.addError(details);
						}
					} else {
						logger.error("JSON data is empty");
						importItem.addError("[chunk #" + chunkIndex + "] JSON data is empty for chunk #" + chunkIndex);
					}
				} catch (Throwable e) {
					// FIXME FTO : Remove that catch Throwable !!! (heritage of old TDM import)
					logger.error("Exception occured while sending json to db", e);
					importItem.addError("[chunk #" + chunkIndex + "] Exception occured while sending chunk #" + chunkIndex + " to db: " + e.getMessage());
				}
			}
		}
		
	    /**
	     * Mirror of {@link fr.cs.ikats.temporaldata.business.TemporalDataManager#getTSUID(String, Long, String) TDM.getTSUID}
	     * 
	     * @param metric
	     *            the metric name
	     * @param date
	     *            the end date of the timeseries
	     * @param hashMap
	     *            the tags
	     * @return the TSUID
	     * @throws IkatsWebClientException
	     *             if request cannot be generated or sent
	     */
	    public String getTSUID(String metric, Long date, Map<String, String> hashMap) throws IkatsWebClientException {
	    	
	        // Build the tag map
	        StringBuilder tagSb = new StringBuilder("{");
	        hashMap.forEach((k, v) -> tagSb.append(k).append("=").append(v).append(","));
	        // remove the trailing "," char
	        tagSb.replace(tagSb.lastIndexOf(","), tagSb.length(), "}");
	    	
			String tsuid = null;
			String apiUrl = (String) config.getProperty(ConfigProps.OPENTSDB_API_URL); 
			String url = apiUrl
			        + urlBuilder.generateMetricQueryUrl(metric, tagSb.toString(), null, null, null, Long.toString(date), null, "show_tsuids");
			Response webResponse = RequestSender.sendGETRequest(url, null);
			String str = webResponse.readEntity(String.class);
			logger.debug("GET TSUID response : " + str);
			
			Matcher matcher = tsuidPattern.matcher(str);
			if (matcher.matches()) {
			    tsuid = matcher.group(1);
			}
			
			return tsuid;
	    }
	}

	public enum ConfigProps implements ConfigProperties {

		OPENTSDB_API_URL("opentsdb.api.url"),
		OPENTSDB_IMPORT_URL("opentsdb.api.import"),
		IMPORT_CHUNK_SIZE("import.chunk.size");

		// Filename
		public final static String propertiesFile = "opentsdbImport.properties";

		private String propertyName;
		private String defaultValue;

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
