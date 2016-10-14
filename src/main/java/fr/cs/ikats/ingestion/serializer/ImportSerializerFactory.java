package fr.cs.ikats.ingestion.serializer;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.datamanager.client.importer.IImportSerializer;
import fr.cs.ikats.ingestion.exception.IngestionException;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.process.opentsdb.OpenTsdbImportTaskFactory;

/**
 * Class of {@link fr.cs.ikats.temporaldata.business.internal.ImportSerializerFactory}
 * @author ftoral
 */
@Singleton
public class ImportSerializerFactory {
	
	private Logger logger = LoggerFactory.getLogger(OpenTsdbImportTaskFactory.class);
	
	/**
     * instance of Serializers, 
     * "autowired" 
     */
	@EJB
	private Set<IImportSerializer> serializers;

	@Lock(LockType.WRITE)
	public IImportSerializer getSerializer(ImportItem item) throws IngestionException {
		
		String serializerFQN = item.getImportSession().getSerializer();
		
		if (serializerFQN == null || serializerFQN.isEmpty()) {
			logger.error("Serializer not provided for session {}", item.getImportSession().toString());
			throw new IngestionException("Serializer not provided for session " + item.getImportSession().toString()); 
		} else {
			
			// we have to find the serializer FQN into the list of available serializers 
			for (IImportSerializer importSerializer : serializers) {
				String currSerializerFQN = importSerializer.getClass().getName();
				if (currSerializerFQN.equals(serializerFQN)) {
					return importSerializer.clone();
				}
			}
			
			// the serializer is not found in the available list (not bound by JEE ?)
			// try to load it from the class path.
			try {
				@SuppressWarnings("unchecked")
				Class<IImportSerializer> loadClass = (Class<IImportSerializer>) getClass().getClassLoader().loadClass(serializerFQN);
				IImportSerializer importSerializer = loadClass.newInstance();
				serializers.add(importSerializer);
				
				return importSerializer.clone();
				
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				logger.error("Could not load {}", serializerFQN);
				throw new IngestionException("Could not load " + serializerFQN, e); 
			}
		}
	}
	

}