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

package fr.cs.ikats.ingestion.process;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ejb.EJB;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import fr.cs.ikats.ingestion.Configuration;
import fr.cs.ikats.ingestion.IngestionConfig;
import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.exception.IngestionRuntimeException;
import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.util.RegExUtils;

// Review#147170 javadoc classe, constructeur, methodes publiques
public class ImportAnalyser implements Runnable {

    @EJB
    private Configuration config = (Configuration) Configuration.getInstance();

    private Logger logger = LoggerFactory.getLogger(ImportAnalyser.class);

    private ImportSession session;

    // Review#147170 commentaire
    private Pattern pathPatternCompiled;

    // Review#147170 commentaire
    private Map<String, Integer> namedGroups;

    private String funcIdPattern;

    // Review#147170
    public ImportAnalyser(ImportSession session) {
        this.session = session;
    }

    @Override
    public void run() {

        session.getStats().timestampSessionAnalysis(true);

        // Check pathPattern parameter regarding regexp rules
        try {
            pathPatternCompiled = Pattern.compile(session.getPathPattern());
        } catch (PatternSyntaxException pse) {
            // Set session cancelled if we could not use the patter to filter the path
            FormattingTuple arrayFormat = MessageFormatter.arrayFormat("Could not use pathPattern '{}' for session {} dataset {}", new Object[]{session.getPathPattern(), session.getId(), session.getDataset()});
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

        // Stores the funcIdPattern for future use
        funcIdPattern = session.getFuncIdPattern();

        // Finally walk over the directory tree to find the files matching our pathPattern regex and provide them as ImportItems with their tags.
        walkOverDataset();

        // We have analyzed the session and collected all the items to import.
        session.setStatus(ImportStatus.ANALYSED);

        // Provide stats
        session.getStats().timestampSessionAnalysis(false);
        session.getStats().setNumberOfItemsInitial(session.getItemsToImport().size());

    }

    private void walkOverDataset() {

        Path datasetRoot = FileSystems.getDefault().getPath(session.getRootPath());
        if (!datasetRoot.isAbsolute()) {
            // If the path provided is not absolute add the configured root path from general configuatio
            datasetRoot = FileSystems.getDefault().getPath(config.getString(IngestionConfig.IKATS_INGESTER_ROOT_PATH), session.getRootPath());
        }

        if (!datasetRoot.toFile().exists() || !datasetRoot.toFile().isDirectory()) {
            FormattingTuple arrayFormat = MessageFormatter.format("Path '{}' not accessible for dataset '{}'", datasetRoot.toString(), session.getDataset());
            String message = arrayFormat.getMessage();
            logger.error(message);
            session.addError(message);
            session.setStatus(ImportStatus.CANCELLED);
            throw new IngestionRuntimeException(message);
        }

        // Reset the absolute path in the session
        session.rootPath = datasetRoot.toString();

        // walk the tree directories to prepare the CSV files
        // Code base from http://rosettacode.org/wiki/Walk_a_directory/Recursively#Java
        try {
            Files.walk(datasetRoot, FileVisitOption.FOLLOW_LINKS)
                    .filter(path -> path.toFile().isFile())
                    .forEach(path -> createImportSessionItem(path.toFile()));

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

        // Provide the calculated funcId to the item
        createFuncId(item);

        session.getItemsToImport().add(item);
        logger.debug("File {} added to import session of dataset {}", importFile.getName(), session.getDataset());
    }

    /**
     * Based on the {@link ImportSessionDto.pathPattern} definition, extract and store metric and tags and metric
     *
     * @param item    the item on which extract metric and tags.
     * @param matcher the configured engine that will parse item data for the tags and the metric
     *                Review#147170 a finir
     */
    private void extractMetricAndTags(ImportItem item, Matcher matcher) {

        HashMap<String, String> tagsMap = new HashMap<String, String>(namedGroups.size());

        // for each regex named group as a tag name, put the KV pair into the list of tags
        for (String tagName : namedGroups.keySet()) {
            // do not add the 'metric'
            if (!tagName.equalsIgnoreCase(config.getString(IngestionConfig.METRIC_REGEX_GROUPNAME))) {
                tagsMap.put(tagName, matcher.group(tagName));
            }
        }

        // Add the current date as tag to make difference with any previous TS containing the same data
        // This allows to have the desired FID even if the TS already exists
        // this tag is also recorded as a metadata attached to the time serie
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Calendar calendar = new GregorianCalendar();
        String date = sdf.format(calendar.getTime());
        tagsMap.put("ikats_import_date", date);

        item.setTags(tagsMap);
        item.setMetric(matcher.group(config.getString(IngestionConfig.METRIC_REGEX_GROUPNAME)));
    }

    /**
     * Format the functional identifier from pattern information of the session and already discovered tags.
     *
     * @param item the item for which the func id should be calculated
     */
    private void createFuncId(ImportItem item) {
        // format the functional identifier from pattern and tags
        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.putAll(item.getTags());
        valuesMap.put("metric", item.getMetric());
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String funcId = sub.replace(funcIdPattern);
        item.setFuncId(funcId);
    }

}
