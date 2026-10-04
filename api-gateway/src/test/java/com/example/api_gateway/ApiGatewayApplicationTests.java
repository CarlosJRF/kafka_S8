package com.example.api_gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/oauth2/jwks" })
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

	@Autowired
	WebTestClient client;

	@Test
	void apiRequiresBearerToken() {
		this.client.get().uri("/api/stats").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void healthIsPublic() {
		this.client.get().uri("/actuator/health").exchange().expectStatus().isOk();
	}

}
