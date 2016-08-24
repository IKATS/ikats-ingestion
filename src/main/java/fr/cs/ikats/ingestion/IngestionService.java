package fr.cs.ikats.ingestion;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ModelManager;

@Startup
@Singleton
@DependsOn({"ModelManager"})
public class IngestionService {

	private List<ImportSession> sessions;
	@EJB public ModelManager modelManager;
	
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
		ImportSession newSession = new ImportSession(session.dataset, session.basePath, session.tags);
		this.sessions.add(newSession);
		logger.info("ImportSession added: (id={}), {}", newSession.getId(), newSession.toString());
		
		return newSession.getId();
	}
	
	@Lock(LockType.WRITE)
	public void removeSession(ImportSessionDto session) {
		boolean removed = this.sessions.removeIf(p -> p.id == session.id);
		if (removed) {
			logger.info("ImportSession removed: (id={}), {}", session.id, session.toString());
		} else {
			logger.error("ImportSession id={} not found", session.id, session.toString());
		}
	}
}
