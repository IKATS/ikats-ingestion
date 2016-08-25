package fr.cs.ikats.ingestion.api;

import java.util.HashMap;

public class ImportSessionDto {

	public int id;
	public String dataset;
	public String rootPath;
	/**
	 * Pattern rules for tags and metric of dataset and TS:<br>
	 * <ul>
	 * <li>The path is described with path separators '/'.</li>
	 * <li>The root of the absolute path is {@link ImportSessionDto.rootPath}
	 * <li>The <b>metric</b> path part is described with <code>{metric}</code></li>
	 * <li>A <b>time serie tag</b> is defined with <code>{tag:tagname}</code> where <code>tagname</code> is the name of the tag and the value, the real path part</li>
	 * <li>An <b>optional time serie tag</b> is defined with <code>{optinal_tag:tagname:defval}</code> where <code>tagname</code> is the name of the tag and the value, the real path part and <code>defval</code> is the default value when tag not present</li>
	 * </ul>
	 * Examples :
	 * <ol>
	 * <li>For Airbus : <code>/DAR/{tag:AircraftIdentifier}/{metric}/raw_{tag:FlightIdentifier}.csv</code>
	 * <li>For EDF : <code>/DAR/{tag:group}/{metric}_?{optinal_tag:level:good}.csv</code>
	 * </li>
	 * </ol>
	 */
	public String pathPattern;
	public HashMap<String, String> tags;

	public ImportSessionDto() {
		super();
	}
}