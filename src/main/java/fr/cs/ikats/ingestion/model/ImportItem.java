package fr.cs.ikats.ingestion.model;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.process.ImportAnalyser;

/**
 * Provides and store information on each items for a data ingestion session.<br>
 * <br>
 * The ImportItem is prepared by the {@link ImportAnalyser} that provides it with target file, metric and tags list.<br>
 * It is attached to the {@link ImportSession} and gets an {@link ImportResult} when import task as finished and  
 */
public class ImportItem {

	private File file;
	private String metric;
	private HashMap<String, String> tags;
	private ImportStatus status;
	@XmlTransient
	private ImportSession importSession;
	private Instant startDate;
	private Instant endDate;
	
	private Logger logger = LoggerFactory.getLogger(ImportItem.class);

	public ImportItem(ImportSession importSession, File importFile) {
		this.importSession = importSession;
		this.file = importFile;
		this.status = ImportStatus.CREATED;
	}

	/**
	 * JAXB callback : set the {@link ImportSession} parent attribute
	 * @param u
	 * @param parent
	 */
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		this.importSession = (ImportSession) parent;
		logger.debug("afterUnmarshal called to set parent importSession (id={}) for item file={}", this.importSession.id, this.file.getName());
	}	


	public void setItemImported() {
		this.importSession.setItemImported(this);
	}
	
	public void setItemInError() {
		this.importSession.setItemInError(this);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public File getFile() {
		return file;
	}


	public void setFile(File file) {
		this.file = file;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public HashMap<String, String> getTags() {
		return tags;
	}

	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
	}

	public ImportStatus getStatus() {
		return status;
	}

	public void setStatus(ImportStatus status) {
		this.status = status;
	}

	public Instant getStartDate() {
		return startDate;
	}
 
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	public Instant getEndDate() {
		return endDate;
	}

	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}
}
