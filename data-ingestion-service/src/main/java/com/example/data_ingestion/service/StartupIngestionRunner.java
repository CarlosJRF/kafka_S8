package com.example.data_ingestion.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Optionally ingests the CSV files as soon as the service is up
 * ({@code ingestion.run-on-startup=true}).
 */
@Component
@ConditionalOnBooleanProperty("ingestion.run-on-startup")
class StartupIngestionRunner implements ApplicationRunner {

	private final IngestionService ingestionService;

	StartupIngestionRunner(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Override
	public void run(ApplicationArguments args) {
		this.ingestionService.ingestAll();
	}

}
