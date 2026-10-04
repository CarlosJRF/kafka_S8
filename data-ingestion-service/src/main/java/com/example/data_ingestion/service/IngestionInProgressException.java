package com.example.data_ingestion.service;

/**
 * Thrown when an ingestion is requested while a previous one is still running.
 */
public class IngestionInProgressException extends RuntimeException {

	public IngestionInProgressException() {
		super("An ingestion is already in progress");
	}

}
