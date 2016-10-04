package fr.cs.ikats.ingestion.api;

import java.util.HashMap;
import java.util.List;

public class ImportSessionDto {

	/** Optional for client requests : only used with API calls for info on session or to change session state */
	public int id;
	public String dataset;
	public String description;
	/** Absolute root path of the dataset on the import server where files are located */
	public String rootPath;
	/**
	 * Pattern rules for defining tags and metric of dataset:<br>
	 * <ul>
	 * <li>The path is described with a regex</li>
	 * <li>The root of the absolute path is {@link ImportSessionDto.rootPath}, and is not included in the pattern
	 * <li>The metric and tags should be matched into regex named groups</li>
	 * <li>The regex <b>should have one metric</b> group defined with: <code>(?&lt;metric&gt;.*)</code></li>
	 * <li>Each tag is defined with a regex group defined with: <code>(?&lt;tagname&gt;.*)</code></li>
	 * </ul>
	 * Examples :
	 * <ol>
	 * <li>For EDF : <code>"\/DAR\/(?&lt;equipement&gt;\w*)\/(?&lt;metric&gt;.*?)(?:_(?&lt;good&gt;bad|good))?\.csv"</code>
	 * <li>For Airbus : <code>"\/DAR\/(?&lt;AircraftIdentifier&gt;\w*)\/(?&lt;metric&gt;.*?)/raw_(?&lt;FlightIdentifier&gt;.*)\.csv"</code>
	 * </li>
	 * </ol>
	 */
	public String pathPattern;
	/** Dataset tags. Not used for now */
	public HashMap<String, String> tags;
	/** List of errors */
	public List<String> errors;

	public ImportSessionDto() {
		super();
	}
}