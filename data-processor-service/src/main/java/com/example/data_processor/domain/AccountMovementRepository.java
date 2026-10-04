package com.example.data_processor.domain;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountMovementRepository extends JpaRepository<AccountMovement, String> {

	List<AccountMovement> findByCuentaIdOrderByFecha(int cuentaId);

	@Query("""
			select m.cuentaId as cuentaId, count(m) as movimientos, sum(m.signedAmount) as saldoNeto
			from AccountMovement m group by m.cuentaId order by m.cuentaId""")
	List<AccountBalance> balances();

	interface AccountBalance {

		int getCuentaId();

		long getMovimientos();

		BigDecimal getSaldoNeto();

	}

}
