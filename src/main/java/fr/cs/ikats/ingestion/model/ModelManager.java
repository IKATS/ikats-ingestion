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

// Review#147170 javadoc classe et methodes publiques
@Singleton
public class ModelManager {

	private static final String IKATS_IMPORT_SESSIONS_FILE = "ikats-import-sessions.xml";
	
    private static ModelManager instance = null; 
    
    public static ModelManager getInstance()  
    {
    	if (instance == null) {
	    	try {
	    	    // Review#147170 explication technique utile: pourquoi passer par JNDI ? ... 
	    	    // Review#147170 le constructeur vide appelé pour @Singleton c'est bien cela ? 
	    		instance = (ModelManager) InitialContext.doLookup("java:global/ikats-ingestion/ModelManager");
	    	} catch (NamingException e) {
	    		Logger logger = LoggerFactory.getLogger(ModelManager.class);
	    		logger.error("Error getting the ModelManager instance", e);
	    	}
    	}
    	
    	return instance;
    }
	
    /**
     * Inner class used as container for the data model to be serialized.
     * Add two extra data : the id sequences. 
     * @author ftoral
     */
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
	
	// Review#147170 corriger nom: marshal en anglais ou mettre un nom plus parlant saveModelXml
	// Review#147170 le verbe anglais est marshal (voir dessous) perso je trouve pas cela tres clair
	// Review#147170 mais je comprendrais si tu passes
	private void marshall() {
		
		try {
			File file = new File(IKATS_IMPORT_SESSIONS_FILE);
			
			Class<?>[] classes = new Class[]{IngestionModel.class, ImportStatus.class};
			
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed then save.
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(model, file);
			
			logger.info("Saved session file: {}", file.getAbsolutePath());

		} catch (JAXBException e) {
		    // Review#147170 logger.error(e)
			e.printStackTrace();
		}
	}
	// Review#147170 corriger nom: unmarshal en anglais ou mettre un nom plus parlant loadModelXml
    // Review#147170 commentaire pour la methode: load a partir du XML des sessions
	// Review#147170 gestion erreurs: on masque des erreurs ... volontaire ? completer les logs ?
	private List<ImportSession> unmarshall() {

		try {
			File file = new File(IKATS_IMPORT_SESSIONS_FILE);
			JAXBContext jaxbContext = JAXBContext.newInstance(IngestionModel.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			model = (IngestionModel) jaxbUnmarshaller.unmarshal(file);

			logger.info("Session loaded");
			
		} catch (UnmarshalException ume) {
			logger.error("Error reading session file: {}", ume.toString());
		} catch (JAXBException je) {
			if (je instanceof UnmarshalException && je.getLinkedException() instanceof FileNotFoundException) {
				logger.warn("Session file not found");
			} else {
				logger.error("Error reading session file: {}", je.toString());
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
