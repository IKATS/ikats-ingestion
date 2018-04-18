/**
 * Copyright 2018 CS Systèmes d'Information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.ikats.ingestion.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.exception.IngestionException;

// Review#147170 javadoc resumant le role de cette classe
// Review#147170 javadoc methodes publiques (y compris getter/setter)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportSession extends ImportSessionDto {
	// Review#147170 remarque cote synchronisation et multithread: la doc de CopyOnWriteArrayList parle de pbs de perfs
    // Review#147170 juste une suggestion de ma part:
    // Review#147170 - la gestion de 3 listes qui vont muter pourrait se reduire à une seule ArrayList, écrite par ImportAnalyser (add)
    // Review#147170 - et tu peux gerer les etats des importItem grace a l'attribut ImportItem::status -completer etat- ! 
    // Review#147170 en rajoutant eventuellement un etat ERROR et quitte  a rendre synchronized son setter ... 
    // Review#147170 - et definir des filtres getAllItems() getImportedItems() getItemsWithError()
    // Review#147170 - apres tu pourrais annoter les filtres plutot que les attributs, si c'est pas un souci ?
	@XmlElementWrapper(name = "toImport")
	@JsonProperty(value = "toImport")
	@XmlElement(name = "item")
	// Review#147170 expliquer/justifier usage CopyOnWriteArrayList ... pas trop couteux ? et sinon synchronized sur 
	// Review#147170 les modifieurs (setItemImported / setItemInError ) ?
	// Review#147170 ... contourne un pb mthread avec ArrayList ?
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
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant startDate;
	@XmlJavaTypeAdapter(value = InstantXmlAdapter.class)
	private Instant endDate;
	/** List of errors */
	@XmlElementWrapper(name = "errors")
	@JsonProperty(value = "errors")
	@XmlElement(name = "message")	
	private List<String> errors;
	
	/** Session stats */
	private SessionStats stats;
	
	@ToStringExclude
	@XmlTransient
	@JsonIgnore
	private Logger logger = LoggerFactory.getLogger(ImportSession.class);
	
	private ImportSession() {
		this.id = ModelManager.getInstance().importSessionSeqNext();
	}
	
	public ImportSession(ImportSessionDto simple) {
		this();
		this.dataset = simple.dataset;
		this.description = simple.description;
		this.rootPath = simple.rootPath;
		this.pathPattern = simple.pathPattern;
		this.funcIdPattern = simple.funcIdPattern;
		this.importer = simple.importer;
		this.serializer = simple.serializer;
		this.status = ImportStatus.CREATED;
		
		this.stats = new SessionStats(simple, this);
	}
	
	/**
	 * Move the importItem from the list of itemsToImport to the itemsImported list.
	 * @param importItem
	 */
	public void setItemImported(ImportItem importItem) {
		boolean removed = itemsToImport.remove(importItem);
		if (!removed) {
			logger.error("Could not remove item {} from list {}", importItem.getFuncId(), "itemsToImport");
			// FIXME throw an exception here
		} 
		else {
			itemsImported.add(importItem);
			logger.debug("Item imported: {}", importItem.getFuncId());
		}
	}
	
	/**
	 * Move the importItem from the list of itemsToImport to the itemsImported list.
	 * @param importItem
	 */
	public void setItemInError(ImportItem importItem) {
		boolean removed = itemsToImport.remove(importItem);
		if (!removed) {
			logger.error("Could not remove item {} from list {}", importItem.getFuncId(), "itemsToImport");
			// FIXME throw an exception here
		} 
		else {
			itemsInError.add(importItem);
			logger.info("Item not imported: {}", importItem.getFuncId());
		}
	}
	
	/**
	 * Move the importItem from the list of itemsInError to the itemsToImport list.
	 * @param importItem
	 * @throws IngestionException 
	 */
	public void setItemToImport(ImportItem importItem) throws IngestionException {
		boolean removed = itemsInError.remove(importItem);
		if (!removed) {
			throw new IngestionException("Could not remove item " + importItem.getFuncId() + "from itemsInError list");
		} 
		else {
			itemsToImport.add(importItem);
			logger.debug("Item reset to import: {}", importItem.getFuncId());
		}
	}
	
	public String toString() {
	    // Review#147170 toString potentiellement enorme: c'est voulu ? je vois qu'il y a un tag exclude possible
		return ToStringBuilder.reflectionToString(this);
	}
	
	// Review#147170 pourquoi avoir separe les getter/setter des attributs de la superclass ? 
	// Review#147170 expliquer si voulu
	// Review#147170 meme Rq pour les autres attr
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
	
	public String getPathPattern() {
		return super.pathPattern;
	}

	public String getFuncIdPattern() {
		return super.funcIdPattern;
	}
	
	public String getImporter() {
		return super.importer;
	}

	public String getSerializer() {
		return super.serializer;
	}

	public Instant getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}
	
	public Instant getEndDate() {
		return endDate;
	}

	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	public List<ImportItem> getItemsToImport() {
		return itemsToImport;
	}

	public List<ImportItem> getItemsImported() {
		return itemsImported;
	}

	public List<ImportItem> getItemsInError() {
		return itemsInError;
	}

	public ImportStatus getStatus() {
		return status;
	}
	// Review#147170 pas de synchronized ici ? 
	public void setStatus(ImportStatus status) {
		this.status = status;
	}
	
	// Review#147170 pas de synchronized ici ?
	public void addError(String error) {
		if (errors == null) {
			errors = new ArrayList<String>();
		}
		
		errors.add(Instant.now() + " - " + error);
	}

	/**
	 * @return the stats
	 */
	public SessionStats getStats() {
		return stats;
	}

}
