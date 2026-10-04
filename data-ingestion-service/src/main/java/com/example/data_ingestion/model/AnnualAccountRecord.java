package com.example.data_ingestion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated row of {@code cuentas_anuales.csv}.
 */
public record AnnualAccountRecord(String sourceId, int cuentaId, LocalDate fecha, String transaccion,
		BigDecimal monto, String descripcion) {
}
