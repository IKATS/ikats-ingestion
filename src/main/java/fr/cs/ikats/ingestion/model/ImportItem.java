package fr.cs.ikats.ingestion.model;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Future;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportItem {

	private File file;
	private String metric;
	private HashMap<String, String> tags;
	private ImportStatus status;
	@XmlTransient
	private ImportSession importSession;
	@XmlTransient
	private Future<ImportResult> futureResult;
	private ImportResult importResult;
	
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
	
	public Future<ImportResult> getFutureResult() {
		return futureResult;
	}
	
	public void setFutureResult(Future<ImportResult> futureResult) {
		this.futureResult = futureResult;
	}
	
	public ImportResult getImportResult() {
		return importResult;
	}

	public void setImportResult(ImportResult importResult) {
		
	}
	
}
