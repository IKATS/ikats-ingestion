package fr.cs.ikats.ingestion.test;

import java.io.File;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.embeddable.EJBContainer;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportSessionIngester;
import fr.cs.ikats.ingestion.process.IngestionProcess;
import fr.cs.ikats.util.concurrent.ExecutorPoolManager;

public class ImportSessionIngesterTest {

    private static EJBContainer ejbContainer;
    
    @EJB
	private static ExecutorPoolManager executorPoolManager;
	
	@Resource(name="java:comp/DefaultManagedThreadFactory") 
	private ManagedThreadFactory threadFactory;

    /***
     * Méthode d'initialisation appelée une seule fois lors de l'exécution
     * des tests de HelloServiceTest.
     * C'est l'endroit idéal pour démarrer l'EJBContainer et récupérer
     * les EJB à tester.
     * @throws NamingException
     */
    @BeforeClass
    public static void init() throws NamingException {
//        Map<String, Object> properties = new HashMap<String, Object>();
//        properties.put(EJBContainer.MODULES, new File("target/classes"));
//        ejbContainer = EJBContainer.createEJBContainer(properties);
        ejbContainer = EJBContainer.createEJBContainer();
        Context ctx = ejbContainer.getContext();

        // le nom JNDI d'un EJB dépend du serveur d'applications utilisé :
        // jboss     : "HelloWorldService/local"
        // glassfish : "java:global/classes.ext/HelloWorldService"
        // 
        String executorPoolManagerServiceName = "java:global/ikats-ingestion/" + ExecutorPoolManager.class.getSimpleName();
        executorPoolManager = (ExecutorPoolManager) ctx.lookup(executorPoolManagerServiceName);
    }
	
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRunThread() throws InterruptedException, NamingException {
		
		int nbItemsToImport = 50;
		int nbItemsImported = 0;
		
		// -- Create an import session
		// Session attibutes
		ImportSessionDto importSessionDto = new ImportSessionDto();
		importSessionDto.dataset = "testRunThreadDS";
		importSessionDto.description = "description testRunThreadDS";
		ImportSession importSession = new ImportSession(importSessionDto);
		// session items
		for (int i = 0; i < nbItemsToImport; i++) {
			ImportItem importItem = new ImportItem(importSession, new File("ts_fakeFile_" + i));
			importSession.getItemsToImport().add(importItem);
		}
		
		// -- Create the ingestion process that do not run, only to pass it as an argument
		// to the ingester !
		IngestionProcess ingestionProcess = new IngestionProcess(importSession, threadFactory, executorPoolManager);
		importSession.setStatus(ImportStatus.RUNNING);
		
		// -- Finally create the runner that we would test
		ImportSessionIngester importSessionIngester = new ImportSessionIngester(ingestionProcess, importSession);
		
		// Start the ingester thread and wait until it to finish
		Thread ingester = new Thread(importSessionIngester);
		ingester.start();
		ingester.join();
		
		// assert nb items = nb item imported
		nbItemsImported = importSession.getItemsImported().size();
		
		Assert.assertTrue("Not all items have been imported", nbItemsToImport == nbItemsImported);
	}

}
