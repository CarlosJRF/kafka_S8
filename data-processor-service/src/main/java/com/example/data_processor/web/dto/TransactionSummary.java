package com.example.data_processor.web.dto;

import java.math.BigDecimal;

public record TransactionSummary(long transacciones, BigDecimal totalCredito, BigDecimal totalDebito,
		BigDecimal neto) {
}
