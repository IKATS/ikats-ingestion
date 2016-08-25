package fr.cs.ikats.ingestion.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class ImportAnalyser implements Runnable {
	
	private Logger logger = LoggerFactory.getLogger(ImportAnalyser.class);

	private ImportSession session;
	
	public ImportAnalyser(ImportSession session) {
		this.session = session;
	}

	@Override
	public void run() {
		
		walkOverDataset();
		session.setStatus(ImportStatus.ANALYSED);
	}

	private void walkOverDataset() {
		
		Path root = FileSystems.getDefault().getPath(session.getRootPath());
		if (!root.toFile().exists() || !root.toFile().isDirectory()) {
			logger.error("Root path not accessible {}", session);
			// FIXME manage exception
			throw new RuntimeException("The root path " + session.getRootPath() + " doesn't exist for dataset " + session.getDataset());
		}
		
		// walk the tree directories to prepare the CSV files
		try {
			Files.walk(root)
			     .filter( path -> path.toFile().isFile())
			     .filter( path -> testFilepath(path) )
			     .forEach( path -> createImportSessionItem(path.toFile()) );
		} catch (IOException e) {
			// FIXME manage exception
			throw new RuntimeException(e);
		}

	}

	/**
	 * Based on the {@link ImportSessionDto.pathPattern} definition test if that is a file to keep as an ImportItem
	 * @param path
	 * @return true if path matches the {@link ImportSessionDto.pathPattern}
	 */
	private boolean testFilepath(Path path) {
		// FIXME pour [#143948] revoir ce filter ou predicate à créer pour filtrer les fichiers correspondant à pathPattern

		// code pour EDF ou airbus
		boolean ok = true;
		
		String pathStr = path.toString();
		//String relativePathStr = pathStr.substring(session.getRootPath().length() - 1);
		
		ok &= pathStr.endsWith(".csv");
		ok &= path.getParent().getParent().toString().equals("DAR");
		//ok &= relativePathStr.startsWith("/DAR/");
		
		return ok;
	}

	private void createImportSessionItem(File importFile) {
		ImportItem item = new ImportItem(importFile);

		// extract metric and tags
		extractMetricAndTags(item);
		
		session.getItemsToImport().add(item);
		logger.debug("File {} added to import session of dataset {}", importFile.getName(), session.getDataset());
	}

	/**
	 * Based on the {@link ImportSessionDto.pathPattern} definition, extract and store metric and tags and metric
	 * @param item the item on which extract metric and tags.
	 */
	private void extractMetricAndTags(ImportItem item) {

		// FIXME pour [#143948] code à remplacer pour la généricité de l'outil d'import
		// parser item.File.getAbsolutePath() avec session.pathPattern
		// remplir item.metric et item.tags en conséquence
		
		// le code suivant permet just le parsing pour EDF.
		
	}
}