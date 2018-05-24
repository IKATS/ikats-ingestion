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

package fr.cs.ikats.ingestion.process.test;

import java.time.Instant;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportItemTaskFactory;

/**
 * Factory which creates a test import task 
 */
public class TestImportTaskFactory implements ImportItemTaskFactory {

	private Logger logger = LoggerFactory.getLogger(TestImportTaskFactory.class);
	
	public TestImportTaskFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Callable<ImportItem> createTask(ImportItem item) {
		
		ImportTask task = new ImportTask(item);
		return task;
	}
	
	/**
	 * 
	 */
	class ImportTask implements Callable<ImportItem> {
		
		private ImportItem importItem;

		public ImportTask(ImportItem importItem) {
			this.importItem = importItem;
			this.importItem.setStatus(ImportStatus.ANALYSED);
		}

		@Override
		public ImportItem call() throws Exception {
			this.importItem.setStartDate(Instant.now());
			logger.info("Importing {} (2.5sec delay)", importItem.getFile());
			importItem.setStatus(ImportStatus.RUNNING);
			Thread.sleep(2500);
			this.importItem.setEndDate(Instant.now());
			importItem.setStatus(ImportStatus.IMPORTED);
			return this.importItem;
		}
			
	}
}
