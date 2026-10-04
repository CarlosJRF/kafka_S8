package com.example.data_processor.kafka;

import com.example.data_processor.event.AnnualAccountEvent;
import com.example.data_processor.event.InterestEvent;
import com.example.data_processor.event.RejectedEvent;
import com.example.data_processor.event.TransactionEvent;
import com.example.data_processor.service.EventProcessingService;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the topics fed by data-ingestion-service. Failures are retried and finally
 * routed to a dead-letter topic by the error handler in {@code KafkaConsumerConfig}.
 */
@Component
class IngestionEventsListener {

	private final EventProcessingService processingService;

	private final JsonMapper jsonMapper;

	IngestionEventsListener(EventProcessingService processingService, JsonMapper jsonMapper) {
		this.processingService = processingService;
		this.jsonMapper = jsonMapper;
	}

	@KafkaListener(topics = "${processor.topics.interests:interests-validated}")
	void onInterest(String payload) {
		this.processingService.process(this.jsonMapper.readValue(payload, InterestEvent.class));
	}

	@KafkaListener(topics = "${processor.topics.transactions:transactions-validated}")
	void onTransaction(String payload) {
		this.processingService.process(this.jsonMapper.readValue(payload, TransactionEvent.class));
	}

	@KafkaListener(topics = "${processor.topics.annual-accounts:annual-accounts-validated}")
	void onAnnualAccount(String payload) {
		this.processingService.process(this.jsonMapper.readValue(payload, AnnualAccountEvent.class));
	}

	@KafkaListener(topics = "${processor.topics.rejected:ingestion-rejected}")
	void onRejected(String payload) {
		this.processingService.process(this.jsonMapper.readValue(payload, RejectedEvent.class));
	}

}
