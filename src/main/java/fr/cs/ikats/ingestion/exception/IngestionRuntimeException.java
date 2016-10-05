package fr.cs.ikats.ingestion.exception;

public class IngestionRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 4977360988665709745L;

	public IngestionRuntimeException() {
	}

	public IngestionRuntimeException(String message) {
		super(message);
	}

	public IngestionRuntimeException(Throwable cause) {
		super(cause);
	}

	public IngestionRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public IngestionRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
