package com.example.data_processor.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload of the {@code annual-accounts-validated} topic.
 */
public record AnnualAccountEvent(String sourceId, int cuentaId, LocalDate fecha, String transaccion,
		BigDecimal monto, String descripcion) {
}
