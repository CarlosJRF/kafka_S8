package com.example.resilient_client.web;

import com.example.resilient_client.client.DataProcessorClient;
import com.example.resilient_client.client.ResilientResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public API of the resilient client. The {@code clientApi} rate limiter protects this
 * service (and the downstream one) from request bursts; errors are mapped in
 * {@link ClientExceptionHandler}.
 */
@RestController
@RequestMapping("/api/client")
public class ClientController {

	private static final String RATE_LIMITER = "clientApi";

	private final DataProcessorClient processor;

	public ClientController(DataProcessorClient processor) {
		this.processor = processor;
	}

	@GetMapping("/stats")
	@RateLimiter(name = RATE_LIMITER)
	public ResilientResponse stats() {
		return this.processor.get("/api/stats");
	}

	@GetMapping("/transactions/summary")
	@RateLimiter(name = RATE_LIMITER)
	public ResilientResponse transactionSummary() {
		return this.processor.get("/api/transactions/summary");
	}

	@GetMapping("/accounts/{cuentaId}/summary")
	@RateLimiter(name = RATE_LIMITER)
	public ResilientResponse accountSummary(@PathVariable int cuentaId) {
		return this.processor.get("/api/accounts/" + cuentaId + "/summary");
	}

}
