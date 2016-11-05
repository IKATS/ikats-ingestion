package fr.cs.ikats.ingestion.process;

import java.util.concurrent.Callable;

import fr.cs.ikats.ingestion.model.ImportItem;

public interface ImportItemTaskFactory {

	Callable<ImportItem> createTask(ImportItem item);

}
