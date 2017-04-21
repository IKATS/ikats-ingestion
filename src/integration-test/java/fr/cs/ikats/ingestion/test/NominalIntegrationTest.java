package fr.cs.ikats.ingestion.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import fr.cs.ikats.ingestion.IngestionService;
import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;

@Category(IntegrationTest.class)
public class NominalIntegrationTest {

	@Rule
	public Timeout globalTimeout = new Timeout(20);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testImportOneTimeserie() throws InterruptedException {
		fail("Not yet implemented");
		
		// Prepare data
		ImportSessionDto sessionInfos = new ImportSessionDto();
		// TODO fill the session information for one TS
		
		// Make the session object
		ImportSession session = new ImportSession(sessionInfos);
				
		// Send command
		IngestionService service = new IngestionService();
		service.addSession(session);
		
		// Waiting loop for the end of import 
		// let the globalTimeout interrupt the loop 
		while (true) {
			if (session.getStatus() == ImportStatus.COMPLETED
					|| session.getStatus() == ImportStatus.CANCELLED
					|| session.getStatus() == ImportStatus.ERROR) {
				break;
			}
			
			Thread.sleep(1000);
		}
		
		// 
		assertTrue(session.getStatus() == ImportStatus.COMPLETED);
		assertEquals(session.getItemsToImport().size(), 0);
		assertEquals(session.getItemsImported().size(), 1);
		assertTrue(session.getItemsImported().get(0).getStatus() == ImportStatus.IMPORTED);
	}

}
