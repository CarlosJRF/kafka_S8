package com.example.data_processor.domain;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, String> {

	@Query("select t.tipo as tipo, count(t) as cantidad, sum(t.monto) as total from BankTransaction t group by t.tipo")
	List<TotalsByType> totalsByType();

	interface TotalsByType {

		String getTipo();

		long getCantidad();

		BigDecimal getTotal();

	}

}
