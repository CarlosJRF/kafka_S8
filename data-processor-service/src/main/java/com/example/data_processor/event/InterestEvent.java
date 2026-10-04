package com.example.data_processor.event;

import java.math.BigDecimal;

/**
 * Payload of the {@code interests-validated} topic.
 */
public record InterestEvent(String sourceId, int cuentaId, String nombre, BigDecimal saldo, Integer edad,
		String tipo) {
}
