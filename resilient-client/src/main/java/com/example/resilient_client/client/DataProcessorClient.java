package com.example.resilient_client.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous consumer of data-processor-service protected by Resilience4j.
 * <p>
 * Each call goes through the {@code dataProcessor} circuit breaker (outer) and retry
 * (inner) instances configured in {@code resilient-client.yml}. When the downstream
 * service fails, is slow or the circuit is OPEN, the fallback answers with the last
 * successful response for that resource, or with a default payload when there is none.
 */
@Component
public class DataProcessorClient {

	private static final Logger log = LoggerFactory.getLogger(DataProcessorClient.class);

	static final String INSTANCE = "dataProcessor";

	private final RestClient restClient;

	private final Map<String, ResilientResponse> lastGoodResponses = new ConcurrentHashMap<>();

	public DataProcessorClient(RestClient processorRestClient) {
		this.restClient = processorRestClient;
	}

	@CircuitBreaker(name = INSTANCE, fallbackMethod = "fallback")
	@Retry(name = INSTANCE)
	public ResilientResponse get(String path) {
		JsonNode body = this.restClient.get().uri(path).retrieve().body(JsonNode.class);
		ResilientResponse response = ResilientResponse.live(body);
		this.lastGoodResponses.put(path, response);
		return response;
	}

	/**
	 * Client errors (404, 403...) are the caller's problem, not an outage: they are
	 * propagated instead of being hidden by cached data.
	 */
	ResilientResponse fallback(String path, HttpClientErrorException ex) {
		throw ex;
	}

	ResilientResponse fallback(String path, Throwable ex) {
		String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
		log.warn("Fallback for {} ({})", path, reason);
		ResilientResponse cached = this.lastGoodResponses.get(path);
		if (cached != null) {
			return new ResilientResponse("cache", cached.retrievedAt(), cached.data(), reason);
		}
		return new ResilientResponse("default", null, JsonNodeFactory.instance.objectNode(), reason);
	}

}
