package com.example.auth_server.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth 2.0 Authorization Server issuing signed JWTs through the Client Credentials grant.
 */
@Configuration
@EnableConfigurationProperties(AuthClientsProperties.class)
public class SecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		http.oauth2AuthorizationServer(
				(authorizationServer) -> http.securityMatcher(authorizationServer.getEndpointsMatcher()))
			.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated());
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.anyRequest().denyAll());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(AuthClientsProperties properties,
			PasswordEncoder passwordEncoder) {
		TokenSettings tokenSettings = TokenSettings.builder().accessTokenTimeToLive(properties.tokenTtl()).build();
		RegisteredClient[] clients = properties.clients()
			.stream()
			.map((client) -> RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(client.clientId())
				.clientSecret(passwordEncoder.encode(client.clientSecret()))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scopes((scopes) -> scopes.addAll(client.scopes()))
				.tokenSettings(tokenSettings)
				.build())
			.toArray(RegisteredClient[]::new);
		return new InMemoryRegisteredClientRepository(clients);
	}

	/**
	 * RSA key pair used to sign the JWTs. Resource servers download the public part from
	 * {@code /oauth2/jwks}. The pair is regenerated on every start, so previously issued
	 * tokens become invalid after a restart.
	 */
	@Bean
	JWKSource<SecurityContext> jwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
			.privateKey((RSAPrivateKey) keyPair.getPrivate())
			.keyID(UUID.randomUUID().toString())
			.build();
		return new ImmutableJWKSet<>(new JWKSet(rsaKey));
	}

	private static KeyPair generateRsaKey() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to generate RSA key pair", ex);
		}
	}

}
