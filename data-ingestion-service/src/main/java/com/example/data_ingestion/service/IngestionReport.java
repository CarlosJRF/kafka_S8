package com.example.data_ingestion.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Summary of one ingestion run, returned by the REST API as execution evidence.
 */
public record IngestionReport(Instant startedAt, Duration duration, long totalAccepted, long totalRejected,
		List<FileReport> files) {

	static IngestionReport of(Instant startedAt, Duration duration, List<FileReport> files) {
		return new IngestionReport(startedAt, duration, files.stream().mapToLong(FileReport::accepted).sum(),
				files.stream().mapToLong(FileReport::rejected).sum(), files);
	}

	/**
	 * @param rejectionReasons how many rows failed each rule (field name to count)
	 */
	public record FileReport(String file, String topic, long read, long accepted, long rejected,
			Map<String, Long> rejectionReasons) {
	}

}
