package com.example.resilient_client.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * HTTP client used to call data-processor-service. Requests are load balanced through
 * Eureka ({@code http://data-processor-service}) and carry a JWT that this service
 * obtains for itself with the Client Credentials grant.
 */
@Configuration(proxyBeanMethods = false)
public class ProcessorClientConfig {

	/** Registration id under {@code spring.security.oauth2.client.registration}. */
	static final String REGISTRATION_ID = "data-processor";

	private static final Authentication SERVICE_PRINCIPAL = new AnonymousAuthenticationToken("resilient-client",
			"resilient-client", AuthorityUtils.createAuthorityList("ROLE_SERVICE"));

	/**
	 * Plain builder for every other client (including the Eureka client itself, which
	 * must not resolve its own registry through the load balancer).
	 */
	@Bean
	@Primary
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	@LoadBalanced
	RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}

	/**
	 * Uses the service-level manager (not the request-bound default) so tokens are
	 * cached per service, independently of the caller of this API.
	 */
	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
			OAuth2AuthorizedClientService authorizedClients) {
		AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				registrations, authorizedClients);
		manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
		return manager;
	}

	@Bean
	RestClient processorRestClient(@LoadBalanced RestClient.Builder builder,
			OAuth2AuthorizedClientManager authorizedClientManager,
			@Value("${client.processor.base-url:http://data-processor-service}") String baseUrl,
			@Value("${client.processor.connect-timeout:2s}") Duration connectTimeout,
			@Value("${client.processor.read-timeout:3s}") Duration readTimeout) {
		OAuth2ClientHttpRequestInterceptor oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
		oauth2.setClientRegistrationIdResolver((request) -> REGISTRATION_ID);
		oauth2.setPrincipalResolver((request) -> SERVICE_PRINCIPAL);
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		return builder.baseUrl(baseUrl).requestFactory(requestFactory).requestInterceptor(oauth2).build();
	}

}
