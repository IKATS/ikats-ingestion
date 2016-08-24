package fr.cs.ikats.ingestion.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import fr.cs.ikats.ingestion.api.ImportSessionDto;

//@RequestScoped
public class ImportSession extends ImportSessionDto {

//	@XmlTransient
//	@Inject private ModelManager modelManager;
	
	private ImportSession() {
//		this.id = modelManager.importSessionSeqNext();
		this.id = ModelManager.getInstance().importSessionSeqNext();
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

	public ImportStatus getStatus() {
		return status;
	}

	public void setStatus(ImportStatus status) {
		this.status = status;
	}
	
}

