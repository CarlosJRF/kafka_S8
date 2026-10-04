package com.example.config_server;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

	@LocalServerPort
	int port;

	@Test
	void servesSharedConfigurationFromConfigRepo() {
		String body = RestClient.create("http://localhost:" + this.port)
			.get()
			.uri("/data-processor-service/default")
			.retrieve()
			.body(String.class);
		assertThat(body).contains("application.yml").contains("eureka.client.service-url.defaultZone");
	}

}
