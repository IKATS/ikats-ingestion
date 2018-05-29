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

package fr.cs.ikats.ingestion;

import fr.cs.ikats.util.configuration.ConfigProperties;

public enum IngestionConfig implements ConfigProperties  {

	// Properties values
	IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY("ingestion.default.importItemTaskFactory", "fr.cs.ikats.ingestion.process.DefaultImportNothingTaskFactory"), 
	IKATS_INGESTER_ROOT_PATH("ikats.ingester.root.path"),
	METRIC_REGEX_GROUPNAME("ikats.ingester.regexp.groupname.metric", "metric");
	
	// Filename
	public final static String propertiesFile = "ingestion.properties";

	private String propertyName;
	private String defaultValue;

	IngestionConfig(String propertyName, String defaultValue) {
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}

	IngestionConfig(String propertyName) {
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
