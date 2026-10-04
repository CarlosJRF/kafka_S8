package com.example.data_processor.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Financial product of a customer (savings, loan or mortgage) with its yearly interest.
 */
@Entity
@Table(name = "interest_products", indexes = @Index(name = "idx_interest_cuenta", columnList = "cuentaId"))
public class InterestProduct {

	@Id
	private String sourceId;

	private int cuentaId;

	private String nombre;

	@Column(precision = 19, scale = 2)
	private BigDecimal saldo;

	private Integer edad;

	private String tipo;

	@Column(precision = 7, scale = 4)
	private BigDecimal tasaAnual;

	@Column(precision = 19, scale = 2)
	private BigDecimal interesAnual;

	private Instant processedAt;

	protected InterestProduct() {
	}

	public InterestProduct(String sourceId, int cuentaId, String nombre, BigDecimal saldo, Integer edad, String tipo,
			BigDecimal tasaAnual, BigDecimal interesAnual) {
		this.sourceId = sourceId;
		this.cuentaId = cuentaId;
		this.nombre = nombre;
		this.saldo = saldo;
		this.edad = edad;
		this.tipo = tipo;
		this.tasaAnual = tasaAnual;
		this.interesAnual = interesAnual;
		this.processedAt = Instant.now();
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public int getCuentaId() {
		return this.cuentaId;
	}

	public String getNombre() {
		return this.nombre;
	}

	public BigDecimal getSaldo() {
		return this.saldo;
	}

	public Integer getEdad() {
		return this.edad;
	}

	public String getTipo() {
		return this.tipo;
	}

	public BigDecimal getTasaAnual() {
		return this.tasaAnual;
	}

	public BigDecimal getInteresAnual() {
		return this.interesAnual;
	}

	public Instant getProcessedAt() {
		return this.processedAt;
	}

}
