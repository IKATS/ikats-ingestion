package fr.cs.ikats.ingestion.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.lang3.builder.ToStringBuilder;

import fr.cs.ikats.ingestion.api.ImportSessionDto;

//@RequestScoped
public class ImportSession extends ImportSessionDto {

	@XmlElementWrapper(name = "toImport")
	@XmlElement(name = "item")
	private List<ImportItem> itemsToImport;
	@XmlElementWrapper(name = "imported")
	@XmlElement(name = "item")
	private List<ImportItem> itemsImported;
	private ImportStatus status;
	private Date startDate;
	
//	@XmlTransient
//	@Inject private ModelManager modelManager;
	
	private ImportSession() {
//		this.id = modelManager.importSessionSeqNext();
		this.id = ModelManager.getInstance().importSessionSeqNext();
	}
	
	public ImportSession(ImportSessionDto simple) {
		this();
		this.dataset = simple.dataset;
		this.rootPath = simple.rootPath;
		this.pathPattern = simple.pathPattern;
		this.tags = simple.tags;
		this.status = ImportStatus.CREATED;
	}
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public int getId() {
		return super.id;
	}

	public String getDataset() {
		return super.dataset;
	}

	public String getDescription() {
		return super.description;
	}
	
	public String getRootPath() {
		return super.rootPath;
	}

	public HashMap<String, String> getTags() {
		return super.tags;
	}

	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public List<ImportItem> getItemsToImport() {
		return itemsToImport;
	}

	public List<ImportItem> getItemsImported() {
		return itemsImported;
	}

	public ImportStatus getStatus() {
		return status;
	}

	public void setStatus(ImportStatus status) {
		this.status = status;
	}

}
