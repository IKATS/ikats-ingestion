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
