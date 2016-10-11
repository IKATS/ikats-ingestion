package fr.cs.ikats.ingestion;

import java.text.MessageFormat;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class Configuration extends CompositeConfiguration {

	private static Configuration instance;

	private Logger logger = LoggerFactory.getLogger(Configuration.class);

	@PostConstruct
	private void init() {
		try {
			Configurations configurations = new Configurations();
			PropertiesConfiguration properties = configurations.properties(getClass().getResource("/" + IngestionConfig.propertiesFile));
			addConfiguration(properties);
			instance = this;
		} catch (ConfigurationException e) {
			logger.error("Not able to load {}. ConfigruationExcerption: {}", IngestionConfig.propertiesFile, e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param propertyKey
	 * @return
	 */
	public static Object getProperty(IngestionConfig propertyKey) {
		if (propertyKey == null) {
			return null;
		}
		
		// call the super implementation to get the property. 
		return instance.getPropertyInternalSuper(propertyKey.getPropertyName());
	}
	
	/**
	 * Use a {@link MessageFormat} internally to format the configuration property 
	 * @param propertyKey
	 * @param values
	 * @return
	 */
	public static String formatProperty(IngestionConfig propertyKey, Object... values) {
		String message = (String) getProperty(propertyKey);
		if (message != null) {
			MessageFormat messageFormat = new MessageFormat(message);
			message = messageFormat.format(values);
		}
		return message;
	}

	/**
	 * Overriden implementation to check that the property is defined by the {@link IngestionConfig} enum.<br>
	 * Avoid direct <code>getProperty(String key)</code> call with a key which is not present in the enum.
	 */
	@Override
	protected Object getPropertyInternal(String key) {
		try {
			// Chek that the property is in the enum list
			IngestionConfig valueEnum = IngestionConfig.valueOf(key);
			return getProperty(valueEnum);
		} catch (IllegalArgumentException | NullPointerException e) {
			logger.debug("Property '{}' not found in {}", key, IngestionConfig.propertiesFile);
		}

		return null;
	}
	
	/**
	 * Wrapper for super.getPropertyInternal(). Used by static method.<br>
	 */
	private Object getPropertyInternalSuper(String key) {
		
		Object value = super.getPropertyInternal(key);
		if (value == null) {
			// In the case when the returned value is null, try return a default value.
			IngestionConfig valueEnum = IngestionConfig.valueOf(key);
			value = valueEnum.getDefaultValue();
		}
		
		return value;
	}

}
