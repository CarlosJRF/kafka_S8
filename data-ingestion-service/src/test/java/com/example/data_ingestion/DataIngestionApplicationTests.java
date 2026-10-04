package com.example.data_ingestion;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.data_ingestion.service.IngestionReport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "ingestion.data-dir=../data" })
@EmbeddedKafka(partitions = 1)
@AutoConfigureMockMvc
class DataIngestionApplicationTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	EmbeddedKafkaBroker broker;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void ingestionRequiresWriteScope() throws Exception {
		this.mvc.perform(post("/api/ingestion/run")).andExpect(status().isUnauthorized());
		this.mvc.perform(post("/api/ingestion/run").with(jwt().authorities(() -> "SCOPE_data.read")))
			.andExpect(status().isForbidden());
	}

	@Test
	void publishesEveryRowEitherAsValidOrRejected() throws Exception {
		this.mvc.perform(post("/api/ingestion/run").with(jwt().authorities(() -> "SCOPE_ingestion.write")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.files.length()").value(3))
			.andExpect(jsonPath("$.files[0].read").value(1000))
			.andExpect(jsonPath("$.totalAccepted").isNumber());

		Map<String, Object> props = KafkaTestUtils.consumerProps(this.broker, "test", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer())
			.createConsumer()) {
			consumer.subscribe(List.of("interests-validated", "transactions-validated",
					"annual-accounts-validated", "ingestion-rejected"));
			List<ConsumerRecord<String, String>> records = new ArrayList<>();
			long deadline = System.currentTimeMillis() + 20_000;
			while (records.size() < 3000 && System.currentTimeMillis() < deadline) {
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2)).forEach(records::add);
			}
			assertThat(records).hasSize(3000);
			assertThat(records).filteredOn((r) -> r.topic().equals("transactions-validated"))
				.isNotEmpty()
				.allSatisfy((r) -> assertThat(r.value()).contains("\"fecha\":\"2024-"));
			assertThat(records).filteredOn((r) -> r.topic().equals("ingestion-rejected"))
				.isNotEmpty()
				.allSatisfy((r) -> assertThat(r.value()).contains("\"reasons\""));
		}
	}

}
