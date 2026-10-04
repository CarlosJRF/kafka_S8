package com.example.data_ingestion.model;

import java.math.BigDecimal;

/**
 * Validated row of {@code intereses.csv}.
 *
 * @param sourceId stable identifier ({@code file:line}) used by consumers for idempotent upserts
 * @param edad customer age, {@code null} when the legacy file did not provide it
 */
public record InterestRecord(String sourceId, int cuentaId, String nombre, BigDecimal saldo, Integer edad,
		String tipo) {
}
