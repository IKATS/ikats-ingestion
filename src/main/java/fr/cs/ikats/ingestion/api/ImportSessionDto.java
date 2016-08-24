package fr.cs.ikats.ingestion.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class ImportSessionDto {

	public int id;
	public String dataset;
	public String basePath;
	public HashMap<String, String> tags;
	@XmlElementWrapper(name = "items")
	@XmlElement(name = "item")
	public List<ImportItem> items;
	protected ImportStatus status;
	protected Date startDate;

	public ImportSessionDto() {
		super();
	}

}