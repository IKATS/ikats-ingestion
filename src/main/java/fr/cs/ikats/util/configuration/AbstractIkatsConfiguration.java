package fr.cs.ikats.util.configuration;

import java.text.MessageFormat;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.IngestionConfig;

public abstract class AbstractIkatsConfiguration<T extends Enum<T> & ConfigProperties> extends CompositeConfiguration {
	
	private T properties;
	
	private Logger logger = LoggerFactory.getLogger(AbstractIkatsConfiguration.class);
		
	public void init(Class<T> propertiesClass) {
		try {
			properties = propertiesClass.getEnumConstants()[0];
			Configurations configurations = new Configurations();
			PropertiesConfiguration conf = configurations.properties(getClass().getResource("/" + properties.getPropertiesFilename()));
			addConfiguration(conf);
		} catch (ConfigurationException e) {
			logger.error("Could not load configuration for {} from properties file {}", getClass().getName(), properties.getPropertiesFilename());
			logger.error("Exception: ", e);
			// throw new IngestionException(e);
		}
	}
	
	/**
	 * Overriden implementation to check that the property is defined by the {@link IngestionConfig} enum.<br>
	 * Avoid direct <code>getProperty(String key)</code> call with a key which is not present in the enum.
	 */
	@Override
	protected Object getPropertyInternal(String key) {
		try {
			// Chek that the property is in the enum list
			@SuppressWarnings("unchecked")
			ConfigProperties valueEnum = (ConfigProperties) Enum.valueOf(properties.getClass(), key);
			return getProperty(valueEnum);
		} catch (IllegalArgumentException | NullPointerException e) {
			logger.debug("Property '{}' not found in {}", key, properties.getPropertiesFilename());
		}
	
		return null;
	}

	/**
	 * 
	 * @param propertyKey
	 * @return
	 */
	public Object getProperty(ConfigProperties propertyKey) {
		if (propertyKey == null) {
			return null;
		}
		
		// call the super implementation to get the property. 
		return getPropertyInternalSuper(propertyKey.getPropertyName());
	}

	/**
	 * Use a {@link MessageFormat} internally to format the configuration property 
	 * @param propertyKey
	 * @param values
	 * @return
	 */
	public String formatProperty(ConfigProperties propertyKey, Object... values) {
		String message = (String) getProperty(propertyKey);
		if (message != null) {
			MessageFormat messageFormat = new MessageFormat(message);
			message = messageFormat.format(values);
		}
		return message;
	}
	
	/**
	 * Wrapper for super.getPropertyInternal(). Used by static method.<br>
	 */
	private Object getPropertyInternalSuper(String key) {
		
		Object value = super.getPropertyInternal(key);
		if (value == null) {
			// In the case when the returned value is null, try return a default value.
			@SuppressWarnings("unchecked")
			ConfigProperties valueEnum = (ConfigProperties) Enum.valueOf(properties.getClass(), key);
			value = valueEnum.getDefaultValue();
		}
		
		return value;
	}
	
}
