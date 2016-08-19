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

import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ModelManager;

@Startup
@Singleton
@DependsOn({"ModelManager"})
public class IngestionService {

	private List<ImportSession> sessions;
	@EJB ModelManager modelManager;

    // The @Startup annotation ensures that this method is
    // called when the application starts up.
    @PostConstruct
    public void applicationStartup() {
    	sessions = modelManager.unmarshall();
    	if (sessions == null) {
    		sessions = new ArrayList<ImportSession>();
    	}
    }
	
    @PreDestroy
    public void applicationShutdown() {
    	modelManager.marshall(sessions);
    }
    
	/**
	 * @return the sessions
	 */
    @Lock(LockType.READ)
	public List<ImportSession> getSessions() {
		return sessions;
	}

	@Lock(LockType.WRITE)
	public void addSession(ImportSession session) {
		this.sessions.add(session);
	}
	
	@Lock(LockType.WRITE)
	public void removeSession(ImportSession session) {
		this.sessions.remove(session);
	}
}
