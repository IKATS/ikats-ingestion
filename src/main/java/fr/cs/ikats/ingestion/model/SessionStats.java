package fr.cs.ikats.ingestion.model;

import java.text.MessageFormat;
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
		"numberOfItemsImported",
		"rateOfImportedItems",
		"numberOfPointsSent",
		"numberOfPointsSuccess",
		"numberOfPointsFailed",
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
	
	@SuppressWarnings("unused")
	private int numberOfItemsToImport;
	
	@SuppressWarnings("unused")
	private int numberOfItemsImported;

	@SuppressWarnings("unused")
	private String rateOfImportedItems;
	
	@SuppressWarnings("unused")
	private long numberOfPointsSent = 0L;
	
	@SuppressWarnings("unused")
	private long numberOfPointsSuccess = 0L;
	
	@SuppressWarnings("unused")
	private long numberOfPointsFailed = 0L;
	
	@SuppressWarnings("unused")
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
	private Run previousRun;
	
	@ToStringExclude
	@XmlTransient
	@JsonIgnore
	private Run currentRun;
	
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
	 * @param toDate if null, set {@link SessionStats#dateStatsUpdated} to {@link Instant#now()}
	 */
	private void updateDateStatsUpdated(Instant toDate) {
		if (toDate == null) {
			dateStatsUpdated = Instant.now();
		}
		else {
			dateStatsUpdated = toDate;
		}
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
		
		updateDateStatsUpdated(null);
	}

	/**
	 * Set the timestamp on session analysis start/end for the current run
	 * @param start
	 */
	public void timestampIngestion(boolean start) {
		Instant now = Instant.now();
		if(start) {
			// Create a new ingestion run in a FIFO
			if (currentRun == null || currentRun.dateIngestionCompleted != null) {
				// This is a first run or the previous run is completed
				// point previous and current
				previousRun = runs.size() == 0 ? new Run() : runs.peek();
				currentRun = new Run();
				// add to top of the pile
				runs.add(currentRun);
				
				
				currentRun.numberOfItemsToImport = sessionLink.getItemsToImport().size();
				
			} else {
				logger.warn("Request to timestamp a new ingestion run (session {}) while the previous runs has no completed date. Updating the dateIngestionStarted property only.", sessionLink.getId());
			}
			
			// set start date to the "peek" of the FIFO 
			currentRun.dateIngestionStarted = now;
		} 
		else {
			// set the ingestion completed date 
			currentRun.dateIngestionCompleted = now;
		}
		
		// set the duration from start of that run
		currentRun.dateIngestionDuration = Duration.between(currentRun.dateIngestionStarted, now);
		
		updateDateStatsUpdated(now);
	}
	
	/**
	 * Add the points to the total of points sent and compute the ingest rate for the current run.
	 * 
	 * @param pointsSent
	 * @param numberOfSuccess
	 * @param numberOfFailed
	 */
	public synchronized void addPoints(long pointsSent, long numberOfSuccess, long numberOfFailed) {

		// update number of points sent / success / failed for the session and for the current run
		this.numberOfPointsSent += pointsSent;
		currentRun.numberOfPointsSent += pointsSent;
		this.numberOfPointsSuccess += numberOfSuccess;
		currentRun.numberOfPointsSuccess += numberOfSuccess;
		this.numberOfPointsFailed += numberOfFailed;
		currentRun.numberOfPointsFailed += numberOfFailed;

		// Compute the duration (now if ingestion not completed)
		Instant toDate = (currentRun.dateIngestionCompleted == null) ? Instant.now() : currentRun.dateIngestionCompleted;
		Duration ingestionDuration = Duration.between(currentRun.dateIngestionStarted, toDate);
		currentRun.dateIngestionDuration = ingestionDuration;
		
		// update average / min / max rate of Points/second
		currentRun.avgPointsPerSecond = (float) currentRun.numberOfPointsSent / (float) ingestionDuration.toMillis() * 1000F ;
		if (currentRun.avgPointsPerSecond > currentRun.maxPointsPerSecond) 
			currentRun.maxPointsPerSecond = currentRun.avgPointsPerSecond;
		if (currentRun.avgPointsPerSecond < currentRun.minPointsPerSecond || currentRun.minPointsPerSecond == 0) 
			currentRun.minPointsPerSecond = currentRun.avgPointsPerSecond;
	
		updateDateStatsUpdated(toDate);
	}
	
	/**
	 * Set the initial number of items to import (computed after session analysis)
	 * @param numberOfItemsInitial
	 */
	public void setNumberOfItemsInitial(int numberOfItemsInitial) {
		this.numberOfItemsInitial = numberOfItemsInitial;
	}
	
	/**
	 * Set the current number of items to import
	 * @param numberOfItemsToImport
	 */
	public void setNumberOfItemsToImport(int numberOfItemsToImport) {
		this.numberOfItemsToImport = numberOfItemsToImport;
		currentRun.numberOfItemsToImport = numberOfItemsToImport;
	}

	/**
	 * Set the number of points imported, both at session level (total) and at the current run level 
	 * @param numberOfItemsImported
	 */
	public void setNumberOfItemsImported(int numberOfItemsImported) {
		// set only the points imported in the current run
		currentRun.numberOfItemsImported = numberOfItemsImported - currentRun.numberOfItemsToImport;
		
		// provide the total information for the session
		this.numberOfItemsImported = numberOfItemsImported;
		float rateOfImportedItems =  (float) numberOfItemsImported / (float) this.numberOfItemsInitial;
		this.rateOfImportedItems = MessageFormat.format("{0,number,#.##%}", rateOfImportedItems);
	}

	/**
	 * Set the number of items in error for the current run
	 * @param numberOfItemsInError
	 */
	public void setNumberOfItemsInError(int numberOfItemsInError) {
		currentRun.numberOfItemsInError = numberOfItemsInError;
	}

	public Duration getDateSessionAnalysisDuration() {
		return dateSessionAnalysisDuration;
	}
	
	public Instant getDateIngestionStarted() {
		return currentRun.dateIngestionStarted;
	}

	public Instant getDateIngestionCompleted() {
		return currentRun.dateIngestionCompleted;
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
			"numberOfItemsToImport",
			"numberOfItemsImported",
			"numberOfItemsInError",
			"numberOfPointsSent",
			"numberOfPointsSuccess",
			"numberOfPointsFailed",
			"avgPointsPerSecond",
			"minPointsPerSecond",
			"maxPointsPerSecond"})
	public static class Run {
		

		@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
		Instant dateIngestionStarted;
		
		@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
		Instant dateIngestionCompleted;
		
		@XmlJavaTypeAdapter(value = DurationXmlAdapter.class)
		Duration dateIngestionDuration;
		
		int numberOfItemsToImport = 0;
		
		int numberOfItemsImported = 0;
		
		int numberOfItemsInError = 0;
		
		long numberOfPointsSent = 0L;
		
		long numberOfPointsSuccess = 0L;
		
		long numberOfPointsFailed = 0L;
		
		float avgPointsPerSecond = 0F;
		
		float maxPointsPerSecond = 0F;

		float minPointsPerSecond = 0F;
	
	}

}
