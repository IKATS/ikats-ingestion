package fr.cs.ikats.ingestion.model;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;

import fr.cs.ikats.datamanager.client.opentsdb.ImportResult;
import fr.cs.ikats.ingestion.process.ImportAnalyser;

/**
 * Provides and store information on each items for a data ingestion session.<br>
 * <br>
 * The ImportItem is prepared by the {@link ImportAnalyser} that provides it with target file, metric and tags list.<br>
 * It is attached to the {@link ImportSession} and gets an {@link ImportResult} when import task as finished and  
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportItem {

	private File file;
	private String metric;
	private String tsuid;
	private String funcId;
	private HashMap<String, String> tags;
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant startDate;
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant endDate;
	private ImportStatus status;
	@ToStringExclude
	@XmlTransient
	@JsonIgnore
	private ImportSession importSession;
	private List<String> errors;
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant importStartDate;
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant importEndDate;
    private long numberOfSuccess = 0;
    private long numberOfFailed = 0;
	
    @ToStringExclude
    @XmlTransient
    @JsonIgnore
	private Logger logger = LoggerFactory.getLogger(ImportItem.class);
	
	@SuppressWarnings("unused")
	private ImportItem() {
		// default constructor
	}

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

	public String getTsuid() {
		return tsuid;
	}

	public void setTsuid(String tsuid) {
		this.tsuid = tsuid;
	}

	public String getFuncId() {
		return funcId;
	}

	public void setFuncId(String funcId) {
		this.funcId = funcId;
	}

	public HashMap<String, String> getTags() {
		if (this.tags == null) {
			this.tags = new HashMap<String, String>();
		}
		return tags;
	}

	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
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

	public ImportStatus getStatus() {
		return status;
	}

	public void setStatus(ImportStatus status) {
		this.status = status;
	}

	public ImportSession getImportSession() {
		return importSession;
	}

	public void setImportSession(ImportSession importSession) {
		this.importSession = importSession;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void addError(String error) {
		if (errors == null) {
			errors = new ArrayList<String>();
		}
		
		errors.add(Instant.now() + " - " + error);
	}

	public Instant getImportStartDate() {
		return importStartDate;
	}

	public void setImportStartDate(Instant importStartDate) {
		this.importStartDate = importStartDate;
	}

	public Instant getImportEndDate() {
		return importEndDate;
	}

	public void setImportEndDate(Instant importEndDate) {
		this.importEndDate = importEndDate;
	}

	public long getNumberOfSuccess() {
		return numberOfSuccess;
	}

	public void addNumberOfSuccess(long numberOfSuccess) {
		this.numberOfSuccess += numberOfSuccess;
	}

	public long getNumberOfFailed() {
		return numberOfFailed;
	}

	public void addNumberOfFailed(long numberOfFailed) {
		this.numberOfFailed += numberOfFailed;
	}

	public ImportSession getSession() {
		return this.importSession;
	}
	
}
