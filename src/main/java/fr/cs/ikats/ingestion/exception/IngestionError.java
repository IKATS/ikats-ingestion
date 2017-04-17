package fr.cs.ikats.ingestion.exception;

import java.lang.Error;
import java.lang.String;
import java.lang.Throwable;

@SuppressWarnings("serial")
public class IngestionError extends Error {

	public IngestionError() {
	}

	public IngestionError(String message) {
		super(message);
	}

	public IngestionError(Throwable cause) {
		super(cause);
	}

	public IngestionError(String message, Throwable cause) {
		super(message, cause);
	}

	public IngestionError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
