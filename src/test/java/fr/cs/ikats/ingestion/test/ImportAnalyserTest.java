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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportAnalyser;

// Review#147170 je sais pas ou l'ecrire mais il manquerait des TU
// Review#147170 - sur IngestionProcess: pour registerDataset() ... => IngestionProcessTest (AC?)
// Review#147170 - sur les web services Sessions (avec des mocks ...) => SessionsTest
// Review#147170 - et a un niveau plus bas: TU sur OpenTsdbImportTaskFactory => OpenTsdbImportTaskFactoryTest

// Review#147170 javadoc succinte sur obj tu
public class ImportAnalyserTest {

	/** Example of Regex for the "pathPattern" in the case of EDF Dataset */
	public final static String EDF_PATH_PATTERN = "\\/DAR\\/(?<equipement>\\w*)\\/(?<metric>.*?)(?:_(?<good>bad|good))?\\.csv";
	private static EJBContainer ejbContainer;
	
	/***
     * Méthode d'initialisation appelée une seule fois lors de l'exécution
     * des tests de HelloServiceTest.
     * C'est l'endroit idéal pour démarrer l'EJBContainer et récupérer
     * les EJB à tester.
     * @throws NamingException
     */
    @BeforeClass
    public static void setUpClass() throws NamingException {
        ejbContainer = EJBContainer.createEJBContainer();
    }
    
    @AfterClass
    public static void tearDownClass() throws NamingException {
    	if (ejbContainer != null) {
    		ejbContainer.getContext().close();
    		ejbContainer.close();
		} 
    }
   
	@Test
	public void testAnalyserNominal() throws URISyntaxException, InterruptedException, IOException {
		
		ImportSessionDto simple = new ImportSessionDto();
		simple.dataset = "DS_FAKE_EDF";
		simple.pathPattern = EDF_PATH_PATTERN;
		Path rootPath = Paths.get(getClass().getResource("/" + simple.dataset).toURI());
		simple.rootPath = rootPath.toAbsolutePath().toString();
		ImportSession importSession = new ImportSession(simple);
		
		// -- Get number of files
		int nbMatchingFiles = countFilesMatching(rootPath, EDF_PATH_PATTERN);
		
		// -- Creates the ingestion process that do not run, only to pass it as an argument
		// to the analyser !
		ImportAnalyser importAnalyser = new ImportAnalyser(importSession);
		
		// Start the analyser thread and wait until it to finish
		Thread analyser = new Thread(importAnalyser);
		analyser.start();
		analyser.join();
		
		// Result 1: the session had to be analysed
		Assert.assertTrue("Import session is not is state ANALYSED", importSession.getStatus() == ImportStatus.ANALYSED);
		// Result 2: the number of files in the path matching the pattern should be the same as those found in session "ItemsToImport" property
		Assert.assertTrue("Number of expect files matching the pattern, not coherent", importSession.getItemsToImport().size() == nbMatchingFiles);
		// Result 3: each item should have a metric
		importSession.getItemsToImport().forEach( item -> {
			Assert.assertTrue("Import item is not in CREATED state", item.getStatus() == ImportStatus.CREATED);
			Assert.assertNotNull("Metric not found", item.getMetric());
			Assert.assertNotNull("No tags for item", item.getTags());
		});
	}

	
	private int countFilesMatching(Path rootPath, String pathPattern) throws IOException {
		
		int count = 0;
		Pattern pattern = Pattern.compile(pathPattern);
		int rootPathEndIndex = rootPath.toFile().toString().length();

		count = (int) Files.walk(rootPath)
			// select only the files
			.filter( path -> path.toFile().isFile() )
			// select the files that fully matches the regex
			.filter( file -> {
				// firstly: get relative path substring 
				File relativePath = new File(file.toString().substring(rootPathEndIndex));
				return pattern.matcher(relativePath.toString()).matches(); 
			})
			// count the results
			.count();
		
		return count;
	}

}
