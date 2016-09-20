package fr.cs.ikats.ingestion.process;

import java.util.concurrent.Callable;

import javax.ejb.Local;

import fr.cs.ikats.ingestion.model.ImportItem;
import fr.cs.ikats.ingestion.model.ImportResult;

@Local
public interface ImportItemTaskFactory {

	Callable<ImportResult> createTask(ImportItem item);

}
