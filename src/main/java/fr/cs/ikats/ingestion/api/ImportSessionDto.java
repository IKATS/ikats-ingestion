package fr.cs.ikats.ingestion.api;

import org.apache.commons.lang3.text.StrSubstitutor;

import fr.cs.ikats.ingestion.IngestionConfig;

public class ImportSessionDto {

	/** Optional for client requests : only used with API calls for info on session or to change session state */
	public int id;
	
	public String dataset;
	
	public String description;
	
	/** Absolute root path of the dataset on the import server where files are located */
	public String rootPath;
	
	/**
	 * Pattern rules for defining tags and metric of dataset:<br>
	 * <ul>
	 * <li>The path is described with a regex</li>
	 * <li>The root of the absolute path is {@link ImportSessionDto#rootPath}, and is not included in the pattern
	 * <li>The metric and tags should be matched into regex named groups</li>
	 * <li>The regex <b>should have one metric</b> group defined with: <code>(?&lt;metric&gt;.*)</code></li>
	 * <li>Each tag is defined with a regex group defined with: <code>(?&lt;tagname&gt;.*)</code></li>
	 * </ul>
	 * Examples :
	 * <ol>
	 * <li>For EDF : <code>"\/DAR\/(?&lt;equipement&gt;\w*)\/(?&lt;metric&gt;.*?)(?:_(?&lt;good&gt;bad|good))?\.csv"</code>
	 * <li>For Airbus : <code>"\/DAR\/(?&lt;AircraftIdentifier&gt;\w*)\/(?&lt;metric&gt;.*?)/raw_(?&lt;FlightIdentifier&gt;.*)\.csv"</code>
	 * </li>
	 * </ol>
	 */
	public String pathPattern;
	
	/**
	 * Pattern for the Functional Identifier.<br>
	 * Follow Apache Commons Lang {@link StrSubstitutor} variable format, with tags names / 'metric' as variables names.<br>
	 * <br>
	 * Examples :
	 * <ol>
	 * <li>For EDF : <code></code></li>
	 * <li>For Airbus : <code>${AircraftIdentifier}_${FlightIdentifier}_${metric}</code></li>
	 * </ol>
	 */
	public String funcIdPattern;
	
	/**
	 * <strong>OPTIONAL</strong><br>
	 * Fully Qualified Name of the java importer used to transfer the Time-Serie data to the IKATS dedicated database.<br>
	 * <br>
	 * Available importers :
	 * <ul>
	 * <li>Default : <code>fr.cs.ikats.ingestion.process.DefaultImportNothingTaskFactory</code><br>
	 * A default implementation used when that property is not found and not default property is defined in the {@link IngestionConfig#propertiesFile} 
	 * at {@link IngestionConfig#IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY}</li>
	 * <li>For OpenTSDB: <code>fr.cs.ikats.ingestion.process.opentsdb.OpenTsdbImportTaskFactory</code></li>
	 * <li>Used for Unit testing: <code>fr.cs.ikats.ingestion.process.test.TestImportTaskFactory</code></li>
	 * </ul>
	 */
	public String importer;
	
	/** 
	 * Set the Fully Qualified Name of the input serializer<br>
	 * Available parsers are:
	 * <ul>
	 * <li><code>fr.cs.ikats.datamanager.client.opentsdb.importer.CommonDataJsonIzer</code></li>
	 * <li><code>fr.cs.ikats.datamanager.client.opentsdb.importer.AirbusDataJsonIzer</code></li>
	 * <li><code>fr.cs.ikats.datamanager.client.opentsdb.importer.EDFDataJsonIzer</code></li>
	 * </ul>
	 */
	public String serializer;

	public ImportSessionDto() {
		super();
	}
}