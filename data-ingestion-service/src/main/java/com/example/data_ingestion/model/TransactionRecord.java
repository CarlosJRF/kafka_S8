package com.example.data_ingestion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated row of {@code transacciones.csv}.
 */
public record TransactionRecord(String sourceId, long id, LocalDate fecha, BigDecimal monto, String tipo) {
}
