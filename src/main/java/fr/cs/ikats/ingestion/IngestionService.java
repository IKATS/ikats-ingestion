package fr.cs.ikats.ingestion;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ModelManager;
import fr.cs.ikats.ingestion.process.IngestionProcess;

@Startup
@Singleton
@DependsOn({"ModelManager"})
public class IngestionService {

	/** List of import sessions to be managed */
	private List<ImportSession> sessions;
	
	/** Pointer to allow persistence of the model */
	@EJB 
	private ModelManager modelManager;
	
	@Resource(name="java:comp/DefaultManagedThreadFactory") 
	private ManagedThreadFactory threadFactory;
	
	private Logger logger = LoggerFactory.getLogger(IngestionService.class);

    // The @Startup annotation ensures that this method is
    // called when the application starts up.
    @PostConstruct
    public void applicationStartup() {
    	
    	logger.debug("IngestionService instancied at application startup");
    	
    	sessions = modelManager.loadModel();
    	if (sessions == null) {
    		sessions = new ArrayList<ImportSession>();
	    	}
	    }
		
	@PreDestroy
    public void applicationShutdown() {

    	logger.debug("IngestionService destroyed at application shutdown");
    	modelManager.saveModel(sessions);
    }
    
	/**
	 * @return the sessions
	 */
    @Lock(LockType.READ)
	public List<ImportSession> getSessions() {
		return sessions;
	}

	@Lock(LockType.WRITE)
	public int addSession(ImportSessionDto session) {
		
		ImportSession newSession = new ImportSession(session);
		this.sessions.add(newSession);
		logger.info("ImportSession added: (id={}), {}", newSession.getId(), newSession.toString());
		
		// Start asynchronous import analysis
		startIngestionProcess(newSession);
		
		return newSession.getId();
	}
	
	@Lock(LockType.WRITE)
	public void removeSession(int id) {
		boolean removed = this.sessions.removeIf(p -> p.getId() == id);
		if (removed) {
			logger.info("ImportSession removed: (id={}), sessions list size = {}", id, this.sessions.size());
		} else {
			logger.error("ImportSession id={} not found", id);
		}
	}
	
	public ImportSessionDto getSession(int id) {
		
		ImportSessionDto session = null;
		
		for (ImportSession importSession : sessions) {
			if (importSession.getId() == id) {
				session = importSession;
			}
		}
		
		return session;
	}
	
	private void startIngestionProcess(ImportSession newSession) {
		
		// Start processsing in a thread
		threadFactory.newThread(new IngestionProcess(threadFactory, newSession)).start();
	}
	
}
