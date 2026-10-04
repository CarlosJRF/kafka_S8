package com.example.data_processor.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Yearly account movement coming from {@code cuentas_anuales.csv}. {@link #signedAmount}
 * is positive for deposits and negative for withdrawals, purchases and payments.
 */
@Entity
@Table(name = "account_movements", indexes = @Index(name = "idx_movement_cuenta", columnList = "cuentaId"))
public class AccountMovement {

	@Id
	private String sourceId;

	private int cuentaId;

	private LocalDate fecha;

	private String transaccion;

	@Column(precision = 19, scale = 2)
	private BigDecimal monto;

	@Column(precision = 19, scale = 2)
	private BigDecimal signedAmount;

	private String descripcion;

	private Instant processedAt;

	protected AccountMovement() {
	}

	public AccountMovement(String sourceId, int cuentaId, LocalDate fecha, String transaccion, BigDecimal monto,
			String descripcion) {
		this.sourceId = sourceId;
		this.cuentaId = cuentaId;
		this.fecha = fecha;
		this.transaccion = transaccion;
		this.monto = monto;
		this.signedAmount = "deposito".equals(transaccion) ? monto : monto.negate();
		this.descripcion = descripcion;
		this.processedAt = Instant.now();
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public int getCuentaId() {
		return this.cuentaId;
	}

	public LocalDate getFecha() {
		return this.fecha;
	}

	public String getTransaccion() {
		return this.transaccion;
	}

	public BigDecimal getMonto() {
		return this.monto;
	}

	public BigDecimal getSignedAmount() {
		return this.signedAmount;
	}

	public String getDescripcion() {
		return this.descripcion;
	}

	public Instant getProcessedAt() {
		return this.processedAt;
	}

}
