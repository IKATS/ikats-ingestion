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
