package com.example.data_processor.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Consolidated view of one account: yearly movements plus its interest-bearing products.
 *
 * @param totalesPorTipo sum of the amounts per movement type (deposito, retiro, compra, pago)
 * @param saldoNeto deposits minus withdrawals, purchases and payments
 */
public record AccountSummary(int cuentaId, int movimientos, Map<String, BigDecimal> totalesPorTipo,
		BigDecimal saldoNeto, List<Product> productos, BigDecimal interesAnualTotal) {

	public record Product(String tipo, String titular, BigDecimal saldo, BigDecimal tasaAnual,
			BigDecimal interesAnual) {
	}

}
