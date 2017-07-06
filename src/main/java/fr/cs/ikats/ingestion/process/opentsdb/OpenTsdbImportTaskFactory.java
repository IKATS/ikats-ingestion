package fr.cs.ikats.ingestion.process.opentsdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

import org.json.simple.parser.ParseException;
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
import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.exception.IngestionError;
import fr.cs.ikats.ingestion.exception.IngestionException;
import fr.cs.ikats.ingestion.exception.NoPointsToImportException;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.model.SessionStats;
import fr.cs.ikats.ingestion.process.AbstractImportTaskFactory;
import fr.cs.ikats.util.configuration.ConfigProperties;
import fr.cs.ikats.util.configuration.IkatsConfiguration;

/**
 * Factory which creates an OpenTSDB import task.<br>
 * Embedded the class task {@link ImportTask} that processes one TS.
 * 
 * @author ftoral
 */
public class OpenTsdbImportTaskFactory extends AbstractImportTaskFactory {

	private final static IkatsConfiguration<ConfigProps> config = new IkatsConfiguration<ConfigProps>(ConfigProps.class);
	
    /** Pattern for the tsuid extracting in {@link ImportTask#getTSUID(String, Long, Map)} */
	private final static Pattern tsuidPattern = Pattern.compile(".*tsuids\":\\[\"(\\w*)\"\\].*");

    /** The OpenTSDB client manager instance (from TemporalDataManagerWebApp) */
    private final static DataBaseClientManager urlBuilder = new DataBaseClientManager();

    private final int IMPORT_NB_POINTS_BY_BATCH;
    
	private Logger logger = LoggerFactory.getLogger(OpenTsdbImportTaskFactory.class);

	/**
	 * Default constructor based upon configured IMPORT_CHUNK_SIZE
	 */
	public OpenTsdbImportTaskFactory() {
		IMPORT_NB_POINTS_BY_BATCH = (int) config.getInt(ConfigProps.IMPORT_CHUNK_SIZE);
	}

	/**
	 * {@inheritDoc}
	 */
	public Callable<ImportItem> createTask(ImportItem item) {
		ImportTask task = new ImportTask(item);
		return task;
	}
	
	/**
	 * The ingestion task that pushes a TS cutted in chunks into OpenTSDB
	 * @author ftoral
	 */
	class ImportTask implements Callable<ImportItem> {
		
		private static final int MAX_GETTSUID_TRIES = 6;
		private static final long WAIT_BEFORE_GETTSUID_TRIES = 5000;
		private ImportItem importItem;

		public ImportTask(ImportItem importItem) {
			this.importItem = importItem;
			this.importItem.setStatus(ImportStatus.ANALYSED);
		}

		@Override
		public ImportItem call() {

			IImportSerializer jsonizer = null;
			
			try {
				jsonizer = (IImportSerializer) getSerializer(this.importItem);
				
				// PREREQ- Initialize the reader/jsonizer
				initJsonizer(jsonizer, importItem);

				// Set import running for that item.
				importItem.setStatus(ImportStatus.RUNNING);
				importItem.setImportStartDate(Instant.now());
				
				// 1- Send the TS
				sendItemInChunks(jsonizer);
				importItem.setImportEndDate(Instant.now());
				
				// 2- Provide ImportItem with imported key values
				importItem.setStartDate(Instant.ofEpochMilli(jsonizer.getDates()[0]));
				importItem.setEndDate(Instant.ofEpochMilli(jsonizer.getDates()[1]));

				// 3- Get the resulting TSUID
				String tsuid = getTSUID(importItem.getMetric(), jsonizer.getDates()[0], importItem.getTags());
		        if (tsuid == null || tsuid.isEmpty()) {
		        	
		        	// Run into a strategy of retries to get the TSUID
		        	int tries = 0;
		        	do {
		        		logger.trace("getTSUID retry #{} for item {}", tries + 1, importItem.getFuncId());
		        		Thread.sleep(WAIT_BEFORE_GETTSUID_TRIES);
		        		tsuid = getTSUID(importItem.getMetric(), jsonizer.getDates()[0], importItem.getTags());
		        		tries ++;
		        		
		        	} while ((tsuid == null || tsuid.isEmpty()) && tries <= MAX_GETTSUID_TRIES);

		        	// Test whether or not we finally got the TSUID, if not throw an exception.
		        	if (tsuid == null || tsuid.isEmpty()) {
		        		logger.trace("TSUID not retrieved after {} tries, for item {}", tries - 1, importItem.getFuncId());
		        		throw new IngestionException("Could not get OpenTSDB tsuid for item: " + importItem.getFuncId());
		        	} else {
		        		logger.debug("TSUID retrieved after {} tries, for item {}", tries - 1, importItem.getFuncId());
		        	}
		        } 
		        
		        importItem.setTsuid(tsuid);
		        importItem.setStatus(ImportStatus.IMPORTED);
				
				// Update stats
				SessionStats stats = importItem.getSession().getStats();
				stats.addPoints(jsonizer.getTotalPointsRead(), 
						importItem.getNumberOfSuccess(),
						importItem.getNumberOfFailed());
			}
			catch (IngestionException | IngestionError e) {
				
				logger.error(e.getMessage(), e.getCause());
				importItem.addError("Exception: " + e.getMessage() + ((e.getCause() == null) ? "" : " - Cause: " + e.getCause().toString())) ;
				
				// In the case of a managed exception, we put the item in error mode in order to allow a future ingestion
				// except in the case of there is no points to import  
				if (e instanceof NoPointsToImportException) {
					importItem.setStatus(ImportStatus.CANCELLED);
				} else {
					importItem.setStatus(ImportStatus.ERROR);
				}
			}
			// } catch (IOException | DataManagerException | IkatsWebClientException e) {
			catch (Exception | Error e) {
				
				// We need to catch all exceptions and errors because the thread status could not be managed otherwise.
				logger.error("Error while processing item {} for file {}", importItem.getFuncId(), importItem.getFile().toString());
				
				FormattingTuple arrayFormat = MessageFormatter.format("Exception: {} - Cause: {}", e.toString(), (e.getCause() == null) ? "null" : e.getCause().toString());
				importItem.addError(arrayFormat.getMessage());
				
				// This is a non managed error: Cancel the item
				importItem.setStatus(ImportStatus.CANCELLED);
				
			} 
			finally {
				
				// in any case if open, close the "jsonizer" which consequently should close the file 
				if (jsonizer != null) {	
					jsonizer.close();
				}
			}

			// the import item was provided with all its new properties
			return this.importItem;

		}

		/**
		 * Initialize the reader / jsonizer
		 * @param jsonizer
		 * @param importItem
		 */
		private void initJsonizer(IImportSerializer jsonizer, ImportItem importItem) {
			
			try {
				File itemFile = importItem.getFile();
				BufferedReader bufferedReader = new BufferedReader(new FileReader(itemFile));
				
				jsonizer.init(bufferedReader, itemFile.getPath(), importItem.getMetric(), importItem.getTags());
				
			} catch (FileNotFoundException e) {
				// should not be reached
				logger.error("File not found", e);
			}
			
		}

		/**
		 * <p>Method which is responsible to send the Timeserie to OpenTSDB.</p>
		 * 
		 * <p>Uses the {@link IImportSerializer} declared in {@link ImportSessionDto#serializer} to parse the lines of the file.
		 * That serializer is initialized by {@link #initJsonizer(IImportSerializer, ImportItem)}.</p>
		 * 
		 * <p>The serializer prepares the JSON data to be sent to OpenTSDB based on the 
		 * {@link OpenTsdbImportTaskFactory#IMPORT_NB_POINTS_BY_BATCH configured number of points}</p>
		 * 
		 * <p>A synchronous request is sent to OpenTSDB, i.e. no response is expected until OpenTSDB has processed the request, 
		 * using 'sync=true' and 'sync_timeout' in th query URL (see <a href="http://opentsdb.net/docs/build/html/api_http/put.html#requests">
		 * OpenTSDB HTTP API PUT Request documentation</a>). Then the response is read and parsed to retrieve information like number of points imported,
		 * number of failed, errors, ... All is stored into the {@link ImportItem}.</p>
		 * 
		 * <p>Note that the request and responses are described in <a href="http://opentsdb.net/docs/build/html/api_http/put.html">OpenTSDB HTTP API PUT documentation</a>.
		 * The whole is managed in the IKATS classes : {@link RequestSender} and {@link ResponseParser}</p>
		 * 
		 * <p>
		 * Expected HTTP:
		 * <ul>
		 *   <li>PUT Request: http://opentsdbhost:4242/api//put?details=true&sync=true&sync_timeout=60000</li>
		 *   <li>JSON data: <pre>
		 * [
		 *     {
		 *         "metric": "WS1",
		 *         "timestamp": 1346846400,
		 *         "value": 18,
		 *         "tags": {
		 *            "AircraftIdentifier": "T019"
		 *            "FlightIdentifier": "122"
		 *         }
		 *     },
		 *     {
		 *         "metric": "WS1",
		 *         "timestamp": 1346846400,
		 *         "value": 9,
		 *         "tags": {
		 *            "AircraftIdentifier": "T019"
		 *            "FlightIdentifier": "122"
		 *         }
		 *     }
		 * ]
		 *   </pre>
		 *   </li>
		 * </ul>
		 * </p>
		 * 
		 * @param jsonizer
		 * @throws IOException
		 * @throws DataManagerException
		 * @throws IngestionError 
		 * @throws NoPointsToImportException 
		 */
		private void sendItemInChunks(IImportSerializer jsonizer) throws DataManagerException, IngestionError, NoPointsToImportException {
			// Create an aggregated ImportResult for the entire item
			int chunkIndex = 0;
			int emptyChuncks = 0;

			// loop to submit import request for each TS chunk
			while (jsonizer.hasNext()) {
				chunkIndex ++;
				try {
					String json = jsonizer.next(IMPORT_NB_POINTS_BY_BATCH);
					if (json != null && !json.isEmpty()) {
						String url = (String) config.getString(ConfigProps.OPENTSDB_IMPORT_URL);
						logger.trace("Sending chunk #{} for item {}", chunkIndex, importItem.getFuncId());
						Response response = RequestSender.sendPUTJsonRequest(url, json);
						ImportResult result = ResponseParser.parseImportResponse(response);
						logger.trace("Import finished for chunk #{} of item {}", chunkIndex, importItem.getFuncId());
						
						// Aggregate the result of this chunk into the item result
						importItem.addNumberOfSuccess(result.getNumberOfSuccess());
						importItem.addNumberOfFailed(result.getNumberOfFailed());
						for (Entry<String, String> error : result.getErrors().entrySet()) {
							String details = "[chunk #" + chunkIndex + "] " + error.getValue();
							importItem.addError(details);
						}
					} else {
						FormattingTuple arrayFormat = MessageFormatter.format("Item {} | chunk #{} - No data to import", importItem.getFuncId(), chunkIndex);
						logger.error(arrayFormat.getMessage());
						importItem.addError(arrayFormat.getMessage());
						emptyChuncks ++;
					}
				} catch (IkatsWebClientException | ParseException e) {
					FormattingTuple arrayFormat = MessageFormatter.format("Item {} | Exception occured with TSDB exchange", importItem.getFuncId());
					throw new IngestionError(arrayFormat.getMessage(), e);
				} catch (IOException ioe) {
					FormattingTuple arrayFormat = MessageFormatter.format("Item {} | I/O Exception readig file {}", importItem.getFuncId(), importItem.getFile().getPath());
					throw new IngestionError(arrayFormat.getMessage(), ioe);
				}
			}
			
			if (chunkIndex == 1 && emptyChuncks == 1) {
				// Special case where the CSV File is empty, no need to go further in the import process. Raise an exception to manage it in the call()
				throw new NoPointsToImportException(importItem);
			}
		}
		
	    /**
	     * Mirror of {@link fr.cs.ikats.temporaldata.business.TemporalDataManager#getTSUID(String, Long, String) TDM.getTSUID}
	     * 
	     * @param metric
	     *            the metric name
	     * @param startDate
	     *            the start date of the timeserie
	     * @param tags
	     *            the tags
	     * @return the TSUID
	     * @throws IkatsWebClientException
	     *             if request cannot be generated or sent
	     */
	    public String getTSUID(String metric, Long startDate, Map<String, String> tags) throws IkatsWebClientException {
	    	
	        // Build the tag map
	        StringBuilder tagSb = new StringBuilder("{");
	        // do not build the query part if there are no tags
	        if (tags.size() > 0) {
	        	tags.forEach((k, v) -> tagSb.append(k).append("=").append(v).append(","));
	        	// remove the trailing "," char
	        	tagSb.replace(tagSb.lastIndexOf(","), tagSb.length(), "}");
	        } 
	        else {
	        	// no tags, the ingest part has used the metric as a tag (OpenTSDB requirement for at least one tag)
	        	tagSb.append("metric=").append(metric).append("}");
	        }
	    	
			String tsuid = null;
			String apiUrl = (String) config.getProperty(ConfigProps.OPENTSDB_API_URL); 
			String url = apiUrl
			        + urlBuilder.generateMetricQueryUrl(metric, tagSb.toString(), "sum", null, null, Long.toString(startDate), Long.toString(startDate+1), "show_tsuids");
			Response webResponse = RequestSender.sendGETRequest(url, null);
			String str = webResponse.readEntity(String.class);
			logger.trace("GET TSUID response : " + str);
			
			Matcher matcher = tsuidPattern.matcher(str);
			if (matcher.matches()) {
			    tsuid = matcher.group(1);
			}
			logger.trace("TSUID extracted: <{}>", tsuid);
			
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
