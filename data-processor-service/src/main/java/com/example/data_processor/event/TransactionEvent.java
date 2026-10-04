package com.example.data_processor.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload of the {@code transactions-validated} topic.
 */
public record TransactionEvent(String sourceId, long id, LocalDate fecha, BigDecimal monto, String tipo) {
}
