package fr.cs.ikats.ingestion.process;

import java.util.concurrent.Callable;

import javax.ejb.Local;

import fr.cs.ikats.ingestion.model.ImportItem;

@Local
public interface ImportItemTaskFactory {

	Callable<ImportItem> createTask(ImportItem item);

}
