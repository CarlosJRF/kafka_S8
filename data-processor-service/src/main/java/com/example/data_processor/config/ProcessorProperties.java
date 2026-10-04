package com.example.data_processor.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the event processor ({@code processor.*}).
 *
 * @param interestRates annual rate applied to each product type (e.g. {@code ahorro: 0.025})
 */
@ConfigurationProperties(prefix = "processor")
public record ProcessorProperties(Map<String, BigDecimal> interestRates) {

	public ProcessorProperties {
		interestRates = (interestRates != null) ? Map.copyOf(interestRates) : Map.of();
	}

	public BigDecimal rateFor(String tipo) {
		return this.interestRates.getOrDefault(tipo, BigDecimal.ZERO);
	}

}
