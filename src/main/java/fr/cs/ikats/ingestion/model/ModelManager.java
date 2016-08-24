package fr.cs.ikats.ingestion.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ModelManager {

	private static final String IKATS_IMPORT_SESSIONS_FILE = "ikats-import-sessions.xml";
	
    private static ModelManager instance = null; 
    
    public static ModelManager getInstance()  
    {
    	if (instance == null) {
	    	try {
	    		instance = (ModelManager) InitialContext.doLookup("java:global/ikats-ingestion/ModelManager");
	    	} catch (NamingException e) {
	    		Logger logger = LoggerFactory.getLogger(ModelManager.class);
	    		logger.error("Error getting the ModelManager instance", e);
	    	}
    	}
    	
    	return instance;
    }
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class IngestionModel {
		int importSessionSeq = 1;
		long importItemSeq = 1;
		// XmLElementWrapper generates a wrapper element around XML epresentation
		@XmlElementWrapper(name = "sessions", required = true)
		// XmlElement sets the name of the entities
		@XmlElement(name = "session")
		List<ImportSession> sessions;
	}
	
	private IngestionModel model = new IngestionModel();
	
	private Logger logger = LoggerFactory.getLogger(ModelManager.class);

	public List<ImportSession> loadModel() {
		
		File file = new File(IKATS_IMPORT_SESSIONS_FILE);
		logger.info("Database file: {}", file.getAbsolutePath());
		
		if (!file.exists()) {
			return null; 
		} else {
			return unmarshall();
		}
	}
	
	public void saveModel(List<ImportSession> sessions) {
		if (sessions == null) {
			// prevent the null case
			sessions = new ArrayList<ImportSession>(0);
		}
		// Attach the list of sessions to be saved in the model.
		model.sessions = sessions;
		
		// Persist the model into the XML file
		marshall();
	}
	
	private void marshall() {
		
		try {
			File file = new File(IKATS_IMPORT_SESSIONS_FILE);
			
			Class<?>[] classes = new Class[]{IngestionModel.class, ImportStatus.class};
			
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(model, file);
			jaxbMarshaller.marshal(model, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	private List<ImportSession> unmarshall() {

		try {
			File file = new File(IKATS_IMPORT_SESSIONS_FILE);
			JAXBContext jaxbContext = JAXBContext.newInstance(IngestionModel.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			model = (IngestionModel) jaxbUnmarshaller.unmarshal(file);
			System.out.println(model);
		} catch (UnmarshalException ume) {
			
		} catch (JAXBException je) {
			if (je instanceof UnmarshalException && je.getLinkedException() instanceof FileNotFoundException) {
				
			} else {
				logger.error(je.getLocalizedMessage());
			}
		} 
		
		return model.sessions;
	}

	/**
	 * Manage the sequence for id {@link ImportSession.id} 
	 * @return incremented id
	 */
	public int importSessionSeqNext() {
		int returnId = model.importSessionSeq;
		model.importSessionSeq++;
		return returnId;
	}

	/**
	 * Manage the sequence for id {@link ImportItem.id} 
	 * @return incremented id
	 */
	public long importItemSeqNext() {
		long returnId = model.importItemSeq;
		model.importItemSeq++;
		return returnId;
	}
	
}
