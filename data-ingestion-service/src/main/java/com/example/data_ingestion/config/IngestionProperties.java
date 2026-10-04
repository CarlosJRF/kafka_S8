package com.example.data_ingestion.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the CSV ingestion pipeline ({@code ingestion.*}).
 *
 * @param dataDir folder containing the legacy CSV files
 * @param runOnStartup whether a full ingestion runs automatically when the service starts
 * @param topics Kafka topics the validated and rejected records are published to
 */
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(@DefaultValue("../data") Path dataDir, @DefaultValue("false") boolean runOnStartup,
		@DefaultValue Topics topics) {

	public record Topics(@DefaultValue("interests-validated") String interests,
			@DefaultValue("transactions-validated") String transactions,
			@DefaultValue("annual-accounts-validated") String annualAccounts,
			@DefaultValue("ingestion-rejected") String rejected, @DefaultValue("3") int partitions) {
	}

}
