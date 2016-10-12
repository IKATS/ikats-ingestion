package fr.cs.ikats.ingestion;

import fr.cs.ikats.util.configuration.ConfigProperties;

public enum IngestionConfig implements ConfigProperties  {

	// Properties values
	IKATS_DATASET_API_URL("ikats.api.url.dataset"),
	IKATS_DATASET_API_URL_2("ikats.api.url.dataset.import"),
	IKATS_DEFAULT_IMPORTITEM_TASK_FACTORY("ingestion.default.importItemTaskFactory", "fr.cs.ikats.ingestion.process.DefaultImportNothingTaskFactory");
	
	// Filename
	public final static String propertiesFile = "ingestion.properties";
	
	// Mandatory default constructor
	IngestionConfig() {};

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
