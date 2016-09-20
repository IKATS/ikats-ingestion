package fr.cs.ikats.ingestion.model;

public class ImportResult extends fr.cs.ikats.datamanager.client.opentsdb.ImportResult {

	private ImportStatus status;

	private ImportItem importItem;

	public boolean isCompleted() {
		return status == ImportStatus.COMPLETED;
	}

	public ImportItem getImportItem() {
		return importItem;
	}

	public void setImportItem(ImportItem importItem) {
		this.importItem = importItem;
	}

}
