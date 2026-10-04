package com.example.auth_server.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 clients registered in memory, bound from the {@code auth.*} properties.
 */
@ConfigurationProperties(prefix = "auth")
public record AuthClientsProperties(Duration tokenTtl, List<Client> clients) {

	public record Client(String clientId, String clientSecret, List<String> scopes) {
	}

}
