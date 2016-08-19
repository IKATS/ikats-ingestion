package fr.cs.ikats.ingestion.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class ImportSessionDto {

	protected int id;
	protected String dataset;
	protected String basePath;
	protected HashMap<String, String> tags;
	protected List<ImportItem> items;
	protected Enum<ImportStatus> status;
	protected Date startDate;

	public ImportSessionDto() {
		super();
	}

}