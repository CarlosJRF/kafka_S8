package com.example.data_processor;

import java.time.Duration;

import com.example.data_processor.domain.AccountMovementRepository;
import com.example.data_processor.domain.BankTransactionRepository;
import com.example.data_processor.domain.InterestProductRepository;
import com.example.data_processor.domain.RejectedRowRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.auto-offset-reset=earliest", "spring.kafka.consumer.group-id=test",
		"spring.datasource.url=jdbc:h2:mem:processor", "spring.jpa.hibernate.ddl-auto=create-drop",
		"processor.interest-rates.ahorro=0.025", "processor.interest-rates.prestamo=0.12" })
@EmbeddedKafka(partitions = 1, topics = { "interests-validated", "transactions-validated",
		"annual-accounts-validated", "ingestion-rejected", "interests-validated.DLT" })
@AutoConfigureMockMvc
class DataProcessorApplicationTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	KafkaTemplate<String, String> kafka;

	@Autowired
	InterestProductRepository interests;

	@Autowired
	BankTransactionRepository transactions;

	@Autowired
	AccountMovementRepository movements;

	@Autowired
	RejectedRowRepository rejected;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void consumesEventsIdempotentlyAndExposesSummaries() throws Exception {
		this.kafka.send("interests-validated", "intereses.csv:2", """
				{"sourceId":"intereses.csv:2","cuentaId":105,"nombre":"Jane Smith","saldo":10000,"edad":40,"tipo":"ahorro"}""");
		this.kafka.send("annual-accounts-validated", "cuentas_anuales.csv:2", """
				{"sourceId":"cuentas_anuales.csv:2","cuentaId":105,"fecha":"2024-03-24","transaccion":"deposito","monto":3000,"descripcion":null}""");
		this.kafka.send("annual-accounts-validated", "cuentas_anuales.csv:3", """
				{"sourceId":"cuentas_anuales.csv:3","cuentaId":105,"fecha":"2024-06-21","transaccion":"compra","monto":500,"descripcion":"Compra en tienda"}""");
		// Same event delivered twice must not create a duplicate row.
		this.kafka.send("annual-accounts-validated", "cuentas_anuales.csv:3", """
				{"sourceId":"cuentas_anuales.csv:3","cuentaId":105,"fecha":"2024-06-21","transaccion":"compra","monto":500,"descripcion":"Compra en tienda"}""");
		this.kafka.send("transactions-validated", "transacciones.csv:2", """
				{"sourceId":"transacciones.csv:2","id":1,"fecha":"2024-06-30","monto":3000,"tipo":"credito"}""");
		this.kafka.send("ingestion-rejected", "transacciones.csv:4", """
				{"sourceId":"transacciones.csv:4","file":"transacciones.csv","line":4,"raw":{"id":"3","tipo":"invalid"},"reasons":["tipo: desconocido ('invalid')"]}""");
		// Malformed payload goes to the dead-letter topic and does not block the partition.
		this.kafka.send("interests-validated", "broken", "{not json");
		this.kafka.send("interests-validated", "intereses.csv:3", """
				{"sourceId":"intereses.csv:3","cuentaId":106,"nombre":"Bob Johnson","saldo":7000,"edad":null,"tipo":"prestamo"}""");

		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
			assertThat(this.interests.count()).isEqualTo(2);
			assertThat(this.movements.count()).isEqualTo(2);
			assertThat(this.transactions.count()).isEqualTo(1);
			assertThat(this.rejected.count()).isEqualTo(1);
		});

		this.mvc.perform(get("/api/accounts/105/summary").with(jwt().authorities(() -> "SCOPE_data.read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.movimientos").value(2))
			.andExpect(jsonPath("$.saldoNeto").value(2500))
			.andExpect(jsonPath("$.productos[0].interesAnual").value(250))
			.andExpect(jsonPath("$.interesAnualTotal").value(250));
		this.mvc.perform(get("/api/stats").with(jwt().authorities(() -> "SCOPE_data.read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rechazadasPorArchivo['transacciones.csv']").value(1));
		this.mvc.perform(get("/api/accounts/999/summary").with(jwt().authorities(() -> "SCOPE_data.read")))
			.andExpect(status().isNotFound());
	}

	@Test
	void requiresReadScope() throws Exception {
		this.mvc.perform(get("/api/stats")).andExpect(status().isUnauthorized());
		this.mvc.perform(get("/api/stats").with(jwt().authorities(() -> "SCOPE_other")))
			.andExpect(status().isForbidden());
	}

}
