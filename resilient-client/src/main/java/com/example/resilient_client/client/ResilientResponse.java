package com.example.resilient_client.client;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

/**
 * Envelope returned by every endpoint so the caller can tell live data from a fallback.
 *
 * @param source {@code live} (downstream answered), {@code cache} (last good answer) or
 * {@code default} (no cached answer available)
 * @param retrievedAt when {@code data} was obtained from data-processor-service
 * @param error why the fallback was used, {@code null} for live responses
 */
public record ResilientResponse(String source, Instant retrievedAt, JsonNode data, String error) {

	static ResilientResponse live(JsonNode data) {
		return new ResilientResponse("live", Instant.now(), data, null);
	}

}
