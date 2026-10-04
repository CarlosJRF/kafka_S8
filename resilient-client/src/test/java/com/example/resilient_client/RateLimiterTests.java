package com.example.resilient_client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.config.import=", "spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.security.oauth2.client.registration.data-processor.provider=auth-server",
		"spring.security.oauth2.client.registration.data-processor.client-id=test",
		"spring.security.oauth2.client.registration.data-processor.authorization-grant-type=client_credentials",
		"spring.security.oauth2.client.provider.auth-server.token-uri=http://localhost:9/oauth2/token",
		"client.processor.base-url=http://localhost:9", "resilience4j.retry.instances.dataProcessor.max-attempts=1",
		"resilience4j.ratelimiter.instances.clientApi.limit-for-period=2",
		"resilience4j.ratelimiter.instances.clientApi.limit-refresh-period=1m",
		"resilience4j.ratelimiter.instances.clientApi.timeout-duration=0s" })
@AutoConfigureMockMvc
class RateLimiterTests {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void rejectsRequestsAboveTheLimitWith429() throws Exception {
		for (int i = 0; i < 2; i++) {
			this.mvc.perform(get("/api/client/stats").with(jwt().authorities(() -> "SCOPE_data.read")))
				.andExpect(status().isOk());
		}
		this.mvc.perform(get("/api/client/stats").with(jwt().authorities(() -> "SCOPE_data.read")))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.status").value(429));
	}

}
