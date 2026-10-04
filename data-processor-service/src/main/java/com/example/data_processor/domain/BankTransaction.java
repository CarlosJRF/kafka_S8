package com.example.data_processor.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credit or debit transaction coming from {@code transacciones.csv}.
 */
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

	@Id
	private String sourceId;

	@Column(unique = true)
	private long transactionId;

	private LocalDate fecha;

	@Column(precision = 19, scale = 2)
	private BigDecimal monto;

	private String tipo;

	private Instant processedAt;

	protected BankTransaction() {
	}

	public BankTransaction(String sourceId, long transactionId, LocalDate fecha, BigDecimal monto, String tipo) {
		this.sourceId = sourceId;
		this.transactionId = transactionId;
		this.fecha = fecha;
		this.monto = monto;
		this.tipo = tipo;
		this.processedAt = Instant.now();
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public long getTransactionId() {
		return this.transactionId;
	}

	public LocalDate getFecha() {
		return this.fecha;
	}

	public BigDecimal getMonto() {
		return this.monto;
	}

	public String getTipo() {
		return this.tipo;
	}

	public Instant getProcessedAt() {
		return this.processedAt;
	}

}
