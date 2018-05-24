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

package fr.cs.ikats.ingestion.test;

import java.io.File;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.embeddable.EJBContainer;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.*;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportSessionIngester;
import fr.cs.ikats.ingestion.process.IngestionProcess;
import fr.cs.ikats.util.concurrent.ExecutorPoolManager;
// Review#147170 j'aimerais bien que tu me presentes les tests
@RunWith(PowerMockRunner.class)
// Ignore standards framework classes and the IKATS class model in order to avoid PowerMock create objects and confusing Hibernate 
@PowerMockIgnore({"com.sun.*", "java.lang.*", "javax.*", "org.*", "fr.cs.ikats.ts.*", "fr.cs.ikats.metadata.*"})
@PrepareForTest(ImportSessionIngester.class)
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
    public static void setUpClass() throws NamingException {
    	// Start EJB Container
        ejbContainer = EJBContainer.createEJBContainer();
        
        // Get the executor pool from container to manage the import threads
        Context ctx = ejbContainer.getContext();
        // Review#147170 plus simplement "java:global/ikats-ingestion/ExecutorPoolManager"
        String executorPoolManagerServiceName = "java:global/ikats-ingestion/" + ExecutorPoolManager.class.getSimpleName();
        executorPoolManager = (ExecutorPoolManager) ctx.lookup(executorPoolManagerServiceName);
    }
	
    @AfterClass
    public static void tearDownClass() throws NamingException {
    	if (ejbContainer != null) {
    		ejbContainer.getContext().close();
    		ejbContainer.close();
		} 
    }
    
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	@PowerMockIgnore()
	public void testRunThread() throws InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		// Mock : do not execute the following method during test
		suppress(method(ImportSessionIngester.class, "registerFunctionalIdent", ImportItem.class));
		suppress(method(ImportSessionIngester.class, "registerMetadata", ImportItem.class));
		suppress(method(ImportSessionIngester.class, "registerItemInDataset", ImportItem.class));
		
		int nbItemsToImport = 10;
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
