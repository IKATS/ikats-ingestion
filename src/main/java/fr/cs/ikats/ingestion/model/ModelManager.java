package fr.cs.ikats.ingestion.model;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Singleton;

@Singleton
public class ModelManager {
	
	private int importSessionSeq = 0;
	private long importItemSeq = 0;
	
	public void marshall(List<ImportSession> sessions) {
		if (sessions == null) {
			// prevent the null case
			sessions = new ArrayList<ImportSession>(0);
		}
	}

	public List<ImportSession> unmarshall() {

		IngestionModel model = new IngestionModel();

		// TODO unmarshall		
		
		importSessionSeq = model.importSessionSeq;
		importItemSeq = model.importItemSeq;
	
		return model.sessions;
	}
	
	protected int importSessionSeqNext() {
		int returnId = importSessionSeq;
		importSessionSeq++;
		return returnId;
	}
	
	protected long importItemSeqNext() {
		long returnId = importItemSeq;
		importItemSeq++;
		return returnId;
	}
	
	class IngestionModel {
		int importSessionSeq = 0;
		long importItemSeq = 0;
		List<ImportSession> sessions;
	}
}
