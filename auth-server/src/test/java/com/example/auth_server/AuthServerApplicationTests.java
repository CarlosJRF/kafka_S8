package com.example.auth_server;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "eureka.client.enabled=false" })
@AutoConfigureMockMvc
class AuthServerApplicationTests {

	@Autowired
	MockMvc mvc;

	@Test
	void issuesJwtWithClientCredentials() throws Exception {
		this.mvc.perform(post("/oauth2/token").with(httpBasic("banking-client", "banking-secret"))
				.param("grant_type", "client_credentials")
				.param("scope", "data.read"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").isNotEmpty())
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.scope").value("data.read"));
	}

	@Test
	void rejectsInvalidClientSecret() throws Exception {
		this.mvc.perform(post("/oauth2/token").with(httpBasic("banking-client", "wrong"))
				.param("grant_type", "client_credentials"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsScopeNotGrantedToClient() throws Exception {
		this.mvc.perform(post("/oauth2/token").with(httpBasic("resilient-client", "resilient-secret"))
				.param("grant_type", "client_credentials")
				.param("scope", "ingestion.write"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void exposesPublicKeys() throws Exception {
		this.mvc.perform(get("/oauth2/jwks")).andExpect(status().isOk()).andExpect(jsonPath("$.keys[0].kty").value("RSA"));
	}

}
