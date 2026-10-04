package com.example.resilient_client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.config.import=", "spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.security.oauth2.client.registration.data-processor.provider=auth-server",
		"spring.security.oauth2.client.registration.data-processor.client-id=test",
		"spring.security.oauth2.client.registration.data-processor.client-secret=test",
		"spring.security.oauth2.client.registration.data-processor.authorization-grant-type=client_credentials",
		"spring.security.oauth2.client.provider.auth-server.token-uri=http://localhost:9/oauth2/token",
		"resilience4j.circuitbreaker.circuit-breaker-aspect-order=1", "resilience4j.retry.retry-aspect-order=2",
		"resilience4j.circuitbreaker.instances.dataProcessor.sliding-window-size=4",
		"resilience4j.circuitbreaker.instances.dataProcessor.minimum-number-of-calls=2",
		"resilience4j.circuitbreaker.instances.dataProcessor.failure-rate-threshold=50",
		"resilience4j.circuitbreaker.instances.dataProcessor.wait-duration-in-open-state=60s",
		"resilience4j.circuitbreaker.instances.dataProcessor.ignore-exceptions=org.springframework.web.client.HttpClientErrorException",
		"resilience4j.retry.instances.dataProcessor.max-attempts=3",
		"resilience4j.retry.instances.dataProcessor.wait-duration=10ms",
		"resilience4j.retry.instances.dataProcessor.ignore-exceptions=org.springframework.web.client.HttpClientErrorException",
		"resilience4j.ratelimiter.instances.clientApi.limit-for-period=1000",
		"resilience4j.ratelimiter.instances.clientApi.limit-refresh-period=1s",
		"management.endpoints.web.exposure.include=health,circuitbreakers" })
@AutoConfigureMockMvc
class ResilientClientApplicationTests {

	/** Fake data-processor-service whose status code each test controls. */
	static final HttpServer downstream;

	static final AtomicInteger status = new AtomicInteger(200);

	static final AtomicInteger hits = new AtomicInteger();

	static {
		try {
			downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		downstream.createContext("/", ResilientClientApplicationTests::handle);
		downstream.start();
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	CircuitBreakerRegistry circuitBreakers;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@BeforeEach
	void reset() {
		this.circuitBreakers.circuitBreaker("dataProcessor").reset();
		status.set(200);
		hits.set(0);
	}

	@AfterAll
	static void stopDownstream() {
		downstream.stop(0);
	}

	@Test
	void returnsLiveDataThenCachedDataWhenDownstreamFails() throws Exception {
		this.mvc.perform(get("/api/client/stats").with(reader()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("live"))
			.andExpect(jsonPath("$.data.transacciones").value(42));

		status.set(503);
		hits.set(0);
		this.mvc.perform(get("/api/client/stats").with(reader()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("cache"))
			.andExpect(jsonPath("$.data.transacciones").value(42))
			.andExpect(jsonPath("$.error").isNotEmpty());
		assertThat(hits).as("retry attempts before falling back").hasValue(3);
	}

	@Test
	void opensCircuitAfterRepeatedFailuresAndStopsCallingDownstream() throws Exception {
		status.set(500);
		for (int i = 0; i < 2; i++) {
			this.mvc.perform(get("/api/client/transactions/summary").with(reader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("default"));
		}
		assertThat(this.circuitBreakers.circuitBreaker("dataProcessor").getState())
			.isEqualTo(CircuitBreaker.State.OPEN);

		hits.set(0);
		this.mvc.perform(get("/api/client/transactions/summary").with(reader()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.startsWith("CallNotPermittedException")));
		assertThat(hits).as("an OPEN circuit short-circuits the call").hasValue(0);

		this.mvc.perform(get("/actuator/circuitbreakers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.circuitBreakers.dataProcessor.state").value("OPEN"));
	}

	@Test
	void propagatesNotFoundWithoutTrippingTheCircuit() throws Exception {
		status.set(404);
		for (int i = 0; i < 3; i++) {
			this.mvc.perform(get("/api/client/accounts/999/summary").with(reader()))
				.andExpect(status().isNotFound());
		}
		assertThat(hits).as("4xx responses are not retried").hasValue(3);
		assertThat(this.circuitBreakers.circuitBreaker("dataProcessor").getState())
			.isEqualTo(CircuitBreaker.State.CLOSED);
	}

	@Test
	void requiresReadScope() throws Exception {
		this.mvc.perform(get("/api/client/stats")).andExpect(status().isUnauthorized());
	}

	private static RequestPostProcessor reader() {
		return SecurityMockMvcRequestPostProcessors.jwt().authorities(() -> "SCOPE_data.read");
	}

	private static void handle(HttpExchange exchange) throws IOException {
		hits.incrementAndGet();
		byte[] body = "{\"transacciones\":42}".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status.get(), body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	@TestConfiguration
	static class FakeDownstreamConfig {

		@Bean
		@Primary
		RestClient testProcessorRestClient() {
			// Same request factory as production: no transport-level retries hiding Resilience4j.
			return RestClient.builder()
				.baseUrl("http://localhost:" + downstream.getAddress().getPort())
				.requestFactory(new SimpleClientHttpRequestFactory())
				.build();
		}

	}

}
