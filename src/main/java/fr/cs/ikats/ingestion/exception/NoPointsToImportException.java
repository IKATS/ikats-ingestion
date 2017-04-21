package fr.cs.ikats.ingestion.exception;

import fr.cs.ikats.ingestion.model.ImportItem;

@SuppressWarnings("serial")
public class NoPointsToImportException extends IngestionException {

	public NoPointsToImportException(ImportItem importItem) {
		super("Item " + importItem.getFuncId() + " - No points to import");
	}

}
