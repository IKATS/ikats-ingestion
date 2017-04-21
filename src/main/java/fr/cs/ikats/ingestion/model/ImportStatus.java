package fr.cs.ikats.ingestion.model;

// Review#147170 peut etre scinder les status de process et ceux des ImportItem ? cela peut etre plus clair
public enum ImportStatus {
	
	CREATED,
	ANALYSED,
	DATASET_REGISTERED,
	RUNNING,
    IMPORTED,	
	ERROR,
	CANCELLED,
	COMPLETED

}
