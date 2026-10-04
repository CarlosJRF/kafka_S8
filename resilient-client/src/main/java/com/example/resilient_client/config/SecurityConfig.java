package com.example.resilient_client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server for the public API; the actuator endpoints that show the
 * circuit breaker state are left open for monitoring.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf((csrf) -> csrf.disable())
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/error", "/actuator/health/**", "/actuator/info", "/actuator/circuitbreakers/**",
						"/actuator/circuitbreakerevents/**", "/actuator/retries/**", "/actuator/ratelimiters/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/client/**").hasAuthority("SCOPE_data.read")
				.anyRequest().denyAll())
			.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()));
		return http.build();
	}

}
