package com.example.data_ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the topic architecture; {@link KafkaAdmin} creates missing topics on startup.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaTopicsConfig {

	@Bean
	KafkaAdmin.NewTopics ingestionTopics(IngestionProperties properties) {
		IngestionProperties.Topics topics = properties.topics();
		return new KafkaAdmin.NewTopics(topic(topics.interests(), topics.partitions()),
				topic(topics.transactions(), topics.partitions()),
				topic(topics.annualAccounts(), topics.partitions()), topic(topics.rejected(), 1));
	}

	private static NewTopic topic(String name, int partitions) {
		return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
	}

}
