package com.example.data_ingestion.service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.data_ingestion.config.IngestionProperties;
import com.example.data_ingestion.model.RejectedRecord;
import com.example.data_ingestion.validation.RecordValidator;
import com.example.data_ingestion.validation.ValidationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * Reads the legacy CSV files, validates every row and publishes the clean records to
 * their topic. Rows that fail validation go to the rejected-records topic with the list
 * of reasons, so nothing is silently dropped.
 */
@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private static final CSVFormat CSV = CSVFormat.DEFAULT.builder()
		.setHeader()
		.setSkipHeaderRecord(true)
		.setIgnoreEmptyLines(true)
		.setTrim(true)
		.get();

	private final IngestionProperties properties;

	private final RecordValidator validator;

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final JsonMapper jsonMapper;

	private final AtomicBoolean running = new AtomicBoolean();

	private final AtomicReference<IngestionReport> lastReport = new AtomicReference<>();

	public IngestionService(IngestionProperties properties, RecordValidator validator,
			KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
		this.properties = properties;
		this.validator = validator;
		this.kafkaTemplate = kafkaTemplate;
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Runs a full ingestion of the three legacy files.
	 * @throws IngestionInProgressException if another run has not finished yet
	 */
	public IngestionReport ingestAll() {
		if (!this.running.compareAndSet(false, true)) {
			throw new IngestionInProgressException();
		}
		try {
			Instant start = Instant.now();
			IngestionProperties.Topics topics = this.properties.topics();
			List<IngestionReport.FileReport> files = List.of(
					ingest("intereses.csv", topics.interests(), this.validator::validateInterest),
					ingest("transacciones.csv", topics.transactions(), this.validator::validateTransaction),
					ingest("cuentas_anuales.csv", topics.annualAccounts(), this.validator::validateAnnualAccount));
			IngestionReport report = IngestionReport.of(start, Duration.between(start, Instant.now()), files);
			this.lastReport.set(report);
			log.info("Ingestion finished: {} accepted, {} rejected in {}", report.totalAccepted(),
					report.totalRejected(), report.duration());
			return report;
		}
		finally {
			this.running.set(false);
		}
	}

	public Optional<IngestionReport> lastReport() {
		return Optional.ofNullable(this.lastReport.get());
	}

	private <T> IngestionReport.FileReport ingest(String fileName, String topic, RowValidator<T> rowValidator) {
		Path file = this.properties.dataDir().resolve(fileName);
		log.info("Ingesting {} into topic '{}'", file, topic);
		List<CompletableFuture<SendResult<String, String>>> sends = new ArrayList<>();
		Map<String, Long> reasons = new TreeMap<>();
		long read = 0;
		long accepted = 0;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
				CSVParser parser = CSV.parse(reader)) {
			for (CSVRecord row : parser) {
				read++;
				long line = row.getRecordNumber() + 1;
				String sourceId = fileName + ":" + line;
				Map<String, String> values = row.toMap();
				ValidationResult<T> result = rowValidator.validate(sourceId, values);
				if (result instanceof ValidationResult.Valid<T>(T value)) {
					accepted++;
					sends.add(this.kafkaTemplate.send(topic, sourceId, toJson(value)));
				}
				else if (result instanceof ValidationResult.Invalid<T>(List<String> errors)) {
					errors.forEach((error) -> reasons.merge(error.substring(0, error.indexOf(':')), 1L, Long::sum));
					RejectedRecord rejected = new RejectedRecord(sourceId, fileName, line, values, errors);
					sends.add(this.kafkaTemplate.send(this.properties.topics().rejected(), sourceId, toJson(rejected)));
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Unable to read " + file, ex);
		}
		// Wait for the broker acknowledgements so the report reflects what Kafka really stored.
		CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).join();
		return new IngestionReport.FileReport(fileName, topic, read, accepted, read - accepted, reasons);
	}

	private String toJson(Object value) {
		return this.jsonMapper.writeValueAsString(value);
	}

	@FunctionalInterface
	private interface RowValidator<T> {

		ValidationResult<T> validate(String sourceId, Map<String, String> row);

	}

}
