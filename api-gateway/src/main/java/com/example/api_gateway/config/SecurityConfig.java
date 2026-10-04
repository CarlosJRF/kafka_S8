package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Edge security: every {@code /api/**} request must carry a valid JWT issued by the
 * auth-server. The token is relayed untouched to the downstream services, which
 * validate it again and enforce their own scopes.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange((exchanges) -> exchanges
				.pathMatchers("/oauth2/token", "/oauth2/jwks").permitAll()
				.pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.pathMatchers("/api/**").authenticated()
				.anyExchange().denyAll())
			.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()))
			.build();
	}

}
