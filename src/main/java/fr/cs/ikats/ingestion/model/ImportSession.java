package fr.cs.ikats.ingestion.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.cs.ikats.ingestion.api.ImportSessionDto;

//@RequestScoped
public class ImportSession extends ImportSessionDto {

	@XmlElementWrapper(name = "toImport")
	@JsonProperty(value = "toImport")
	@XmlElement(name = "item")
	private CopyOnWriteArrayList<ImportItem> itemsToImport = new CopyOnWriteArrayList<ImportItem>();
	@XmlElementWrapper(name = "imported")
	@JsonProperty(value = "imported")
	@XmlElement(name = "item")
	private CopyOnWriteArrayList<ImportItem> itemsImported = new CopyOnWriteArrayList<ImportItem>();;
	@XmlElementWrapper(name = "inError")
	@JsonProperty(value = "inError")
	@XmlElement(name = "item")
	private CopyOnWriteArrayList<ImportItem> itemsInError = new CopyOnWriteArrayList<ImportItem>();;
	private ImportStatus status;
	private Date startDate;
	
//	@XmlTransient
//	@Inject private ModelManager modelManager;
	
	private Logger logger = LoggerFactory.getLogger(ImportSession.class);
	
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
	
	/**
	 * Move the importItem from the list of itemsToImport to the itemsImported list.
	 * @param importItem
	 */
	public void setItemImported(ImportItem importItem) {
		boolean removed = this.itemsToImport.remove(importItem);
		if (!removed) {
			logger.error("Could not remove item from list; {}", importItem); 
			// FIXME throw an exception here
		} else {
			this.itemsImported.add(importItem);
			logger.debug("Item imported: {}", importItem);
		}
	}
	
	/**
	 * Move the importItem from the list of itemsToImport to the itemsImported list.
	 * @param importItem
	 */
	public void setItemInError(ImportItem importItem) {
		boolean removed = this.itemsToImport.remove(importItem);
		if (!removed) {
			logger.error("Could not remove item from list; {}", importItem);
			// FIXME throw an exception here
		} else {
			this.itemsInError.add(importItem);
			logger.debug("Item not imported: {}", importItem);
		}
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
