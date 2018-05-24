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

package fr.cs.ikats.ingestion.process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.datamanager.client.importer.IImportSerializer;
import fr.cs.ikats.ingestion.exception.IngestionException;
import fr.cs.ikats.ingestion.model.ImportItem;

// Review#147170 javadoc ... expliquer
/**
 * 
 */
public abstract class AbstractImportTaskFactory implements ImportItemTaskFactory {

	private final static Map<String, Class<IImportSerializer>> jsonizerMap = new HashMap<String, Class<IImportSerializer>>();
	
	private Logger logger = LoggerFactory.getLogger(AbstractImportTaskFactory.class);

	/**
	 * Default constructor
	 */
	public AbstractImportTaskFactory() {
		// nothing to do but mandatory when class is loaded dynamically
	}
	
	public abstract Callable<ImportItem> createTask(ImportItem item);
	
	@SuppressWarnings("unchecked")
	public IImportSerializer getSerializer(ImportItem item) throws IngestionException {
		
		String serializerFQN = item.getImportSession().getSerializer();
		// Review#147170 expliquer interet de jsonizerMap pour booster les perfs
		Class<IImportSerializer> importSerializerClass = jsonizerMap.get(serializerFQN);
		
		IImportSerializer serializerInstance;
		
		if (importSerializerClass == null) {
			try {
				
				// load the class matching the serializer name provided by the session
				importSerializerClass = (Class<IImportSerializer>) getClass().getClassLoader().loadClass(serializerFQN);
				// set it available for next access
				jsonizerMap.put(serializerFQN, importSerializerClass);
				
			} catch (ClassNotFoundException e) {
				logger.error("Could not load {}", serializerFQN);
				throw new IngestionException("Could not load " + serializerFQN, e); 
			}
		}
		
		try {
			
			// get a new instance of the serializer
			serializerInstance = importSerializerClass.newInstance();
			
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error("Could not get instace of {}", serializerFQN);
			throw new IngestionException("Could not get instace of " + serializerFQN, e); 
		}
		
		return serializerInstance;
	}

}
