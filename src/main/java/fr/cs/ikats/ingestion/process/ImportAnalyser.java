package fr.cs.ikats.ingestion.process;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.exception.IngestionRuntimeException;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.util.RegExUtils;

// Review#147170 javadoc classe, constructeur, methodes publiques
public class ImportAnalyser implements Runnable {
	
	private static final String METRIC_REGEX_GROUPNAME = "metric";

	private Logger logger = LoggerFactory.getLogger(ImportAnalyser.class);

	private ImportSession session;

	// Review#147170 commentaire
	private Pattern pathPatternCompiled;

	// Review#147170 commentaire
	private Map<String, Integer> namedGroups;
	
	// Review#147170 
	public ImportAnalyser(ImportSession session) {
		this.session = session;
	}

	@Override
	public void run() {
		
		// Check pathPattern parameter regarding regexp rules
		try {
			pathPatternCompiled = Pattern.compile(session.getPathPattern());
		} catch (PatternSyntaxException pse) {
			// Set session cancelled if we could not use the patter to filter the path
			FormattingTuple arrayFormat = MessageFormatter.arrayFormat("Could not use pathPattern '{}' for session {} dataset {}", new Object[] { session.getPathPattern(), session.getId(), session.getDataset() });
        	logger.error(arrayFormat.getMessage());
        	session.addError(arrayFormat.getMessage()); 
			session.setStatus(ImportStatus.CANCELLED);
			// get out the run
			return;
		}
		
		// Check and store the regex groups names : will be tags names.
		try {
			namedGroups = RegExUtils.getNamedGroups(pathPatternCompiled);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			
			FormattingTuple arrayFormat = MessageFormatter.format("Could not get group names from pathPattern regexp for extracting tags. Exception message: '{}'", e.getMessage());
			logger.error(arrayFormat.getMessage(), e);
			session.addError(arrayFormat.getMessage()); 
			session.setStatus(ImportStatus.CANCELLED);
			// get out the run
			return;
		}
		
		// Finally walk over the directory tree to find the files matching our pathPattern regex and provide them as ImportItems with their tags. 
		walkOverDataset();
		
		// We have analyzed the session and collected all the items to import. 
		session.setStatus(ImportStatus.ANALYSED);

	}

	private void walkOverDataset() {
		
		Path root = FileSystems.getDefault().getPath(session.getRootPath());
		if (!root.toFile().exists() || !root.toFile().isDirectory()) {
			FormattingTuple arrayFormat = MessageFormatter.format("Root path not accessible {}", session);
			logger.error(arrayFormat.getMessage());
			session.addError(arrayFormat.getMessage()); 
			session.setStatus(ImportStatus.CANCELLED);
			throw new IngestionRuntimeException("The root path " + session.getRootPath() + " doesn't exist for dataset " + session.getDataset());
		}
		
		// walk the tree directories to prepare the CSV files
		// Code base from http://rosettacode.org/wiki/Walk_a_directory/Recursively#Java
		try {
			Files.walk(root)
				 .filter( path -> path.toFile().isFile() )
				 .forEach( path -> createImportSessionItem(path.toFile()) );
			
			// note on Files nio API, and filters : see explanations here :
			// http://stackoverflow.com/questions/29316310/java-8-lambda-expression-for-filenamefilter/29316408#29316408
		} catch (IOException e) {
			throw new IngestionRuntimeException(e);
		}

	}

	private void createImportSessionItem(File importFile) {
		
		// work only on the path relative to the import session root path
		File relativePath = new File(importFile.getPath().substring(this.session.getRootPath().length()));
		
		Matcher matcher = pathPatternCompiled.matcher(relativePath.getPath());
		if (!matcher.matches()) {
			// the file is not compliant to the pattern, we exclude it.
			return;
		}
		
		// Create item with regard to the current file
		ImportItem item = new ImportItem(this.session, importFile);

		// extract metric and tags
		extractMetricAndTags(item, matcher);
		
		session.getItemsToImport().add(item);
		logger.debug("File {} added to import session of dataset {}", importFile.getName(), session.getDataset());
	}

	/**
	 * Based on the {@link ImportSessionDto.pathPattern} definition, extract and store metric and tags and metric
	 * @param item the item on which extract metric and tags.
	 * @param matcher the 
	 * Review#147170 a finir 
	 */
	private void extractMetricAndTags(ImportItem item, Matcher matcher) {

		HashMap<String, String> tagsMap = new HashMap<String, String>(namedGroups.size());
		
		// for each regex named group as a tag name, put the KV pair into the list of tags
		for (String tagName : namedGroups.keySet()) {
			// do not add the 'metric'
			if (!tagName.equalsIgnoreCase(METRIC_REGEX_GROUPNAME)) {
				tagsMap.put(tagName, matcher.group(tagName));
			}
		}
		
		item.setTags(tagsMap);
		item.setMetric(matcher.group(METRIC_REGEX_GROUPNAME));
	}
}