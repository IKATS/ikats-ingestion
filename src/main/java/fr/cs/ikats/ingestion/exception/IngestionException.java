package fr.cs.ikats.ingestion.exception;

public class IngestionException extends Exception {

	private static final long serialVersionUID = 4977360988665709745L;

	public IngestionException() {
	}

	public IngestionException(String message) {
		super(message);
	}

	public IngestionException(Throwable cause) {
		super(cause);
	}

	public IngestionException(String message, Throwable cause) {
		super(message, cause);
	}

	public IngestionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
