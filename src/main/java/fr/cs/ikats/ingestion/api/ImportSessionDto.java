/**
 * Copyright 2018 CS Systèmes d'Information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.ikats.ingestion.api;

import org.apache.commons.lang3.text.StrSubstitutor;

import fr.cs.ikats.ingestion.IngestionConfig;

// Review#147170 - javadoc de classe 
// Review#147170 - AC: remommer avec nom plus parlant -confusion des roles avec ImportSession-
// Review#147170   Il me semble qu'ici ce sont les definitions de l'import par utilisateur ... plus que l'état d'avancement
public class ImportSessionDto {

	/** Optional for client requests : only used with API calls for info on session or to change session state */
	public int id;
	
	// Review#147170 javadoc dataset et description
	public String dataset;
	
	public String description;
	
	/** 
	 * Root path of the dataset on the import server where files are located.<br>
	 * <ul>
	 *   <li>Could be absolute, in that case, represent the path on the server.</li>
	 *   <li>If relative, the configuration property defined in {@link IngestionConfig#IKATS_INGESTER_ROOT_PATH} will be used as root path.</li>
	 * </ul>  
	 */
	public String rootPath;
	
	// Review#147170 </li> rajoutés
	/**
	 * Pattern rules for defining tags and metric of dataset:<br>
	 * <ul>
	 * <li>The path is described with a regex</li>
	 * <li>The root of the absolute path is {@link ImportSessionDto#rootPath}, and is not included in the pattern</li>
	 * <li>The metric and tags should be matched into regex named groups</li>
	 * <li>The regex <b>should have one metric</b> group defined with: <code>(?&lt;metric&gt;.*)</code></li>
	 * <li>Each tag is defined with a regex group defined with: <code>(?&lt;tagname&gt;.*)</code></li>
	 * </ul>
	 * Examples :
	 * <ol>
	 * <li>For EDF : <code>"\/DAR\/(?&lt;equipement&gt;\w*)\/(?&lt;metric&gt;.*?)(?:_(?&lt;validity&gt;bad|good))?\.csv"</code></li>
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
	 * <li>For EDF : <code>${equipement}_${metric}_${validity}</code></li>
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

	// Review#147170 javadoc
	public ImportSessionDto() {
		super();
	}
}
