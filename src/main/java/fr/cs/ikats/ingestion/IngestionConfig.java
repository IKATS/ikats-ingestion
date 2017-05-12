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
