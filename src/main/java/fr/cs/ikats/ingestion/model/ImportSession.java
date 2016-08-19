package fr.cs.ikats.ingestion.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ejb.EJB;

import fr.cs.ikats.ingestion.api.ImportSessionDto;

public class ImportSession extends ImportSessionDto {

	@EJB private ModelManager modelManager;
	
	private ImportSession() {
		this.id = modelManager.importSessionSeqNext();
	}
	
	public ImportSession(String dataset, String basePath, HashMap<String, String> tags) {
		this();
		this.dataset = dataset;
		this.basePath = basePath;
		this.tags = tags;
	}
	
	public int getId() {
		return id;
	}

	public String getDataset() {
		return dataset;
	}

	public String getBasePath() {
		return basePath;
	}

	public HashMap<String, String> getTags() {
		return tags;
	}

	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public List<ImportItem> getItems() {
		return items;
	}

	public Enum<ImportStatus> getStatus() {
		return status;
	}

	public void setStatus(Enum<ImportStatus> status) {
		this.status = status;
	}
	
}

