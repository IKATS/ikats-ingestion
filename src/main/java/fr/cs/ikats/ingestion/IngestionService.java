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
import fr.cs.ikats.ingestion.exception.IngestionRejectedException;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.model.ModelManager;
import fr.cs.ikats.ingestion.process.IngestionProcess;
import fr.cs.ikats.util.concurrent.ExecutorPoolManager;

// Review#147170 javadoc manquante sur classe et ses methodes publiques

// Review#147170 ajouter pour @DependsOn: (par exemple:
// Review#147170 the @DependsOn assures that ModelManager singleton has been initialized before the @PostConstruct method
// Review#147170 applicationStartup() is called, during this singleton initialization )
@Startup
@Singleton
@DependsOn({"ModelManager"})
public class IngestionService {

	/** List of import sessions to be managed */
	private List<ImportSession> sessions;
	
	/** Pointer to allow persistence of the model */
	@EJB
	private ModelManager modelManager;
	
	@EJB 
	private ExecutorPoolManager executorPoolManager;
	
	// Review#147170 quasi redondant avec la factory de ExecutorPoolManager ? pourquoi 
	// Review#147170 y a t il une difference dans name=... ? 
	@Resource(name="java:comp/DefaultManagedThreadFactory") 
	private ManagedThreadFactory threadFactory;
	
	private Logger logger = LoggerFactory.getLogger(IngestionService.class);

	// Review#147170 renommer avec un role plus parlant ? theIngestionProcess ...  
	private Thread newThread;

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
    // Review#147170 javadoc
	@Lock(LockType.WRITE)
	public int addSession(ImportSessionDto session) {
		
		ImportSession existingSession = getExistingSession(session);
		
		if (existingSession != null) {
			throw new IngestionRejectedException("The import session exist with id " + existingSession.getId());
		}
		
		ImportSession newSession = new ImportSession(session);
		this.sessions.add(newSession);
		logger.info("ImportSession added: (id={}), for dataset {}", newSession.getId(), newSession.getDataset());
		
		// Start asynchronous import analysis
		startIngestionProcess(newSession);
		
		return newSession.getId();
	}
	
	// Review#147170 javadoc
	@Lock(LockType.WRITE)
	public void removeSession(int id) {
		boolean removed = this.sessions.removeIf(p -> p.getId() == id);
		if (removed) {
			logger.info("ImportSession removed: (id={}), sessions list size = {}", id, this.sessions.size());
		} else {
			logger.error("ImportSession id={} not found", id);
		}
	}
	
	// Review#147170 javadoc
	public ImportSessionDto getSession(int id) {
		
		ImportSessionDto session = null;
		
		for (ImportSession importSession : sessions) {
			if (importSession.getId() == id) {
				session = importSession;
			}
		}
		
		return session;
	}
	
	/**
	 * <p>Get the {@link ImportSession} instance relative to the corresponding {@link ImportSessionDto} description provided.</p>
	 * <p>The returned session is compared with the following attributes (in order) :  
	 * <ol>
	 *   <li>{@link ImportSessionDto#dataset}</li>
	 *   <li>{@link ImportSessionDto#pathPattern}</li>
	 *   <li>{@link ImportSessionDto#funcIdPattern}</li>
	 * </ol>
	 * </p>
	 * @param fromSession the description of the session to search
	 * @return an existing session or <code>null</code>
	 */
	public ImportSession getExistingSession(ImportSessionDto fromSession) {
		
		ImportSession existingSession = null;
		
		for (ImportSession session : sessions) {
			if (session.getDataset().equals(fromSession.dataset)) {
				if (session.getPathPattern().equals(fromSession.pathPattern)) {
					if (session.getFuncIdPattern().equals(fromSession.funcIdPattern)) {
						existingSession = session;
						break;
					}
				}
			}
		}
		
		return existingSession;
	}

	/**
	 * Restart the session by getting all non imported items and reset them to the list of items to import, then launch the ingestion process.<br>
	 * The <code>force</code> option force all items to be reseted, otherwise only the items with {@link ImportStatus#ERROR}, which are ingestion "managed" errors are reseted.
	 * 
	 * @param id the id of the session to restart
	 * @param force to restart all items in error.
	 */
	public void restartSession(int id, boolean force) {
		
    	ImportSession session = (ImportSession) getSession(id);
    	
    	// For each item in error, put it in the list of items to import and change its status.
		session.getItemsInError().forEach(itemInError -> {
			if (itemInError.getStatus() == ImportStatus.ERROR || force == true) {
				// reset only item with status ERROR, or with any status in case of force.
				try {
					session.setItemToImport(itemInError);
					itemInError.setStatus(ImportStatus.CREATED);
				} catch (Exception e) {
					logger.error("Error while reseting item {} from InError for restarting ingestion: {}", itemInError.getFuncId(), e.toString());
				}
			}
		});
		
		// Reset the status of the session
		session.setStatus(ImportStatus.DATASET_REGISTERED);
		
		// launch the ingestion
		startIngestionProcess(session);
	}
	
	/**
	 * Start a unique thread for ingestion.<br>
	 * That iteration of IKATS Ingestion support only one ingestion at time.
	 * @param newSession the session describing the dataset to import
	 */
	private void startIngestionProcess(ImportSession newSession) {
		
		// launch only one import session
		if (newThread != null && newThread.isAlive()) {
			throw new IngestionRejectedException("A session is already in process (could only import one session at time)");
		}
		// Review#147170 correction dans le texte
		// TODO in order to ensure fair usage of ExecutorPoolManager, future implementation should take care 
		// of limiting the EPM instance in the scope of one ImportItemTaskFactory.
		// for that:
		//   - EPM should not be a Singleton and should be instanciated at the ImportItemTaskFactory init with dedicated config
		//   - OpenTsdbImportTaskFactory should be Singleton
		//   - ...
		// Then that part of the process could run on multiple sessions and use an EPM to manage that.
		
		// Start processsing in a thread
		newThread = threadFactory.newThread(new IngestionProcess(newSession, threadFactory, executorPoolManager));
		newThread.start();
		
	}
	
}
