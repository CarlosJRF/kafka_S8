package com.example.data_processor.config;

import tools.jackson.core.JacksonException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer error handling: a failing record is retried 3 times one second apart (e.g.
 * while the database is briefly unavailable) and then parked in {@code <topic>.DLT}
 * so the partition keeps flowing. Malformed JSON is sent to the DLT straight away.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

	@Bean
	CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
		DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate),
				new FixedBackOff(1000L, 3L));
		handler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class);
		return handler;
	}

}
