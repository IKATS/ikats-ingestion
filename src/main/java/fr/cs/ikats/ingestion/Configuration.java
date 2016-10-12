package fr.cs.ikats.ingestion;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.util.configuration.AbstractIkatsConfiguration;

/**
 * Ingestion application configuration manager.<br>
 * That class uses the {@link IngestionConfig} definition.
 */
@Startup
@Singleton
public class Configuration extends AbstractIkatsConfiguration<IngestionConfig> {

	private static Configuration instance;
	
	public Logger logger = LoggerFactory.getLogger(Configuration.class);

	@PostConstruct
	private void init() {
		super.init(IngestionConfig.class);
		instance = this;
	}
	
	/**
	 * Dedicated static method to get instance when that JavaEE singleton is not binded into a bean.<br>
	 * This allow to use it every where in the application.
	 * @return the IKATS ingestion application configuration.  
	 */
	public static AbstractIkatsConfiguration<IngestionConfig> getInstance() {
		return instance;
	}
}
