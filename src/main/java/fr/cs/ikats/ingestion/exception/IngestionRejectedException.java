package fr.cs.ikats.ingestion.exception;
// Review#147170 javadoc
public class IngestionRejectedException extends IngestionRuntimeException {

	private static final long serialVersionUID = 4164792876658682601L;
	
	public IngestionRejectedException(String message) {
		super(message);
	}

}
