package fr.cs.ikats.ingestion.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Stack;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.migesok.jaxb.adapter.javatime.DurationXmlAdapter;
import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;

import fr.cs.ikats.ingestion.api.ImportSessionDto;

/**
 * Provide statistical information during ingestion.
 * @author ftoral
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType (propOrder = {
		"dateStatsUpdated",
		"dateSessionCreated",
		"sessionStatus",
		"numberOfItemsInitial",
		"numberOfItemsToImport",
		"dateSessionAnalysisStarted",
		"dateSessionAnalysisCompleted",
		"dateSessionAnalysisDuration",
		"runs",
		"sessionDescriptor"})
public class SessionStats {
	
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant dateSessionCreated;
	
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant dateSessionAnalysisStarted;
	
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant dateSessionAnalysisCompleted;
	
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant dateStatsUpdated;
	
	private int numberOfItemsInitial;
	
	private int numberOfItemsToImport;
	
	private ImportSessionDto sessionDescriptor;

	@XmlJavaTypeAdapter(value = DurationXmlAdapter.class)
	private Duration dateSessionAnalysisDuration;

	@XmlTransient
	@JsonIgnore
	private ImportSession sessionLink;
	
	@XmlElementWrapper(name = "runs")
	@JsonProperty(value = "runs")
	@XmlElement(name = "run")
	private Stack<Run> runs = new Stack<Run>();
	
    @ToStringExclude
    @XmlTransient
    @JsonIgnore
	private Logger logger = LoggerFactory.getLogger(SessionStats.class);
	
	@SuppressWarnings("unused")
	private SessionStats() {
		// default constructor: mandatory for serialization
	}
	
	/**
	 * Create a stat container for the current session.
	 * @param descriptor used for printing session info in stats
	 * @param session used to get the status at runtime
	 */
	public SessionStats(ImportSessionDto descriptor, ImportSession session) {
		sessionDescriptor = descriptor;
		sessionLink = session;
		dateSessionCreated = Instant.now();
	}

	/**
	 * JAXB callback : set the {@link ImportSession} parent attribute
	 * @param u
	 * @param parent
	 */
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		this.sessionLink = (ImportSession) parent;
		logger.debug("afterUnmarshal called to set parent importSession (id={}) for stats", this.sessionLink.id);
	}	

	/**
	 * Updates the date of computed stats
	 */
	private void updateDateStatsUpdated() {
		dateStatsUpdated = Instant.now();
	}

	/**
	 * Directly return the status from the session
	 * @return the current session status
	 */
	@XmlElement
	public ImportStatus getSessionStatus() {
		return this.sessionLink.getStatus();
	}
	
	/**
	 * Set the timestamp on session analysis start/end
	 * @param start
	 */
	public void timestampSessionAnalysis(boolean start) {
		if(start) {
			dateSessionAnalysisStarted = Instant.now();
		} else {
			dateSessionAnalysisCompleted = Instant.now();
			dateSessionAnalysisDuration = Duration.between(dateSessionAnalysisStarted, dateSessionAnalysisCompleted);
		}
		
		updateDateStatsUpdated();
	}

	/**
	 * Set the timestamp on session analysis start/end for the current run
	 * @param start
	 */
	public void timestampIngestion(boolean start) {
		if(start) {
			// Create a new ingestion run in a FIFO
			runs.add(new Run());
			
			// set date to the "peek" of the FIFO 
			runs.peek().dateIngestionStarted = Instant.now();
		} else {
			runs.peek().dateIngestionCompleted = Instant.now();
			runs.peek().dateIngestionDuration = Duration.between(runs.peek().dateIngestionStarted, runs.peek().dateIngestionCompleted);
		}
		
		updateDateStatsUpdated();
	}
	
	/**
	 * Add the points to the total of points sent and compute the ingest rate for the current run.
	 * 
	 * @param pointsSent
	 * @param numberOfSuccess
	 * @param numberOfFailed
	 */
	public synchronized void addPoints(long pointsSent, long numberOfSuccess, long numberOfFailed) {

		runs.peek().numberOfPointsSent += pointsSent;
		Instant toDate = (runs.peek().dateIngestionCompleted == null) ? Instant.now() : runs.peek().dateIngestionCompleted;
		Duration ingestionDuration = Duration.between(runs.peek().dateIngestionStarted, toDate);
		
		runs.peek().ratePointsPerSecond = (float) runs.peek().numberOfPointsSent / (float) ingestionDuration.toMillis() * 1000F ;
		runs.peek().numberOfPointsSuccess += numberOfSuccess;
		runs.peek().numberOfPointsFailed += numberOfFailed;
		
		updateDateStatsUpdated();
	}
	
	public ImportSessionDto getSessionDescriptor() {
		return sessionDescriptor;
	}

	public int getNumberOfItemsInitial() {
		return numberOfItemsInitial;
	}

	public void setNumberOfItemsInitial(int numberOfItemsInitial) {
		this.numberOfItemsInitial = numberOfItemsInitial;
	}

	public int getNumberOfItemsToImport() {
		return numberOfItemsToImport;
	}

	public void setNumberOfItemsToImport(int numberOfItemsToImport) {
		this.numberOfItemsToImport = numberOfItemsToImport;
	}

	public void setNumberOfItemsImported(int numberOfItemsImported) {
		this.runs.peek().numberOfItemsImported = numberOfItemsImported;
	}

	public void setNumberOfItemsInError(int numberOfItemsInError) {
		runs.peek().numberOfItemsInError = numberOfItemsInError;
	}

	public Duration getDateSessionAnalysisDuration() {
		return dateSessionAnalysisDuration;
	}
	
	public Instant getDateIngestionStarted() {
		return runs.peek().dateIngestionStarted;
	}

	public Instant getDateIngestionCompleted() {
		return runs.peek().dateIngestionCompleted;
	}
	
	/**
	 * Stores stats for one run of a session 
	 * @author ftoral
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType (propOrder = {
			"dateIngestionStarted",
			"dateIngestionCompleted",
			"dateIngestionDuration",
			"numberOfItemsImported",
			"numberOfItemsInError",
			"numberOfPointsSent",
			"numberOfPointsSuccess",
			"numberOfPointsFailed",
			"ratePointsPerSecond"})
	public static class Run {
		
		@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
		Instant dateIngestionStarted;
		
		@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
		Instant dateIngestionCompleted;
		
		@XmlJavaTypeAdapter(value = DurationXmlAdapter.class)
		Duration dateIngestionDuration;
		
		int numberOfItemsImported;
		
		int numberOfItemsInError;
		
		int numberOfPointsSent;
		
		long numberOfPointsSuccess;
		
		long numberOfPointsFailed;
		
		float ratePointsPerSecond;
	}

}
