package com.example.resilient_client.web;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Kept outside {@link ClientController} so the rate limiter never wraps the handlers.
 */
@RestControllerAdvice
class ClientExceptionHandler {

	@ExceptionHandler(RequestNotPermitted.class)
	ProblemDetail rateLimited(RequestNotPermitted ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
	}

	@ExceptionHandler(HttpClientErrorException.class)
	ResponseEntity<ProblemDetail> downstreamClientError(HttpClientErrorException ex) {
		return ResponseEntity.status(ex.getStatusCode())
			.body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(), "data-processor-service: " + ex.getStatusText()));
	}

}
