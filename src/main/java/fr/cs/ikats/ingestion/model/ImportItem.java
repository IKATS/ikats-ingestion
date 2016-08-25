package fr.cs.ikats.ingestion.model;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ImportItem {

	private File file;
	private HashMap<String, String> tags;
	private ImportStatus status;

	public ImportItem(File importFile) {
		this.file = importFile;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public File getFile() {
		return file;
	}


	public void setFile(File file) {
		this.file = file;
	}

	public HashMap<String, String> getTags() {
		return tags;
	}

	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
	}

	public ImportStatus getStatus() {
		return status;
	}

	public void setStatus(ImportStatus status) {
		this.status = status;
	}
}
