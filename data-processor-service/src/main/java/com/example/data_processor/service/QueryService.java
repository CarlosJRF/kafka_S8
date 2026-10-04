package com.example.data_processor.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.data_processor.domain.AccountMovement;
import com.example.data_processor.domain.AccountMovementRepository;
import com.example.data_processor.domain.BankTransactionRepository;
import com.example.data_processor.domain.InterestProduct;
import com.example.data_processor.domain.InterestProductRepository;
import com.example.data_processor.domain.RejectedRowRepository;
import com.example.data_processor.web.dto.AccountSummary;
import com.example.data_processor.web.dto.ProcessingStats;
import com.example.data_processor.web.dto.TransactionSummary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model exposed through the REST API.
 */
@Service
@Transactional(readOnly = true)
public class QueryService {

	private final InterestProductRepository interests;

	private final BankTransactionRepository transactions;

	private final AccountMovementRepository movements;

	private final RejectedRowRepository rejected;

	public QueryService(InterestProductRepository interests, BankTransactionRepository transactions,
			AccountMovementRepository movements, RejectedRowRepository rejected) {
		this.interests = interests;
		this.transactions = transactions;
		this.movements = movements;
		this.rejected = rejected;
	}

	public List<AccountMovementRepository.AccountBalance> balances() {
		return this.movements.balances();
	}

	public Optional<AccountSummary> accountSummary(int cuentaId) {
		List<AccountMovement> accountMovements = this.movements.findByCuentaIdOrderByFecha(cuentaId);
		List<InterestProduct> products = this.interests.findByCuentaIdOrderBySourceId(cuentaId);
		if (accountMovements.isEmpty() && products.isEmpty()) {
			return Optional.empty();
		}
		Map<String, BigDecimal> totals = accountMovements.stream()
			.collect(Collectors.groupingBy(AccountMovement::getTransaccion,
					Collectors.reducing(BigDecimal.ZERO, AccountMovement::getMonto, BigDecimal::add)));
		BigDecimal net = accountMovements.stream()
			.map(AccountMovement::getSignedAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal yearlyInterest = products.stream()
			.map(InterestProduct::getInteresAnual)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<AccountSummary.Product> productViews = products.stream()
			.map((p) -> new AccountSummary.Product(p.getTipo(), p.getNombre(), p.getSaldo(), p.getTasaAnual(),
					p.getInteresAnual()))
			.toList();
		return Optional.of(new AccountSummary(cuentaId, accountMovements.size(), totals, net, productViews,
				yearlyInterest));
	}

	public TransactionSummary transactionSummary() {
		Map<String, BankTransactionRepository.TotalsByType> byType = this.transactions.totalsByType()
			.stream()
			.collect(Collectors.toMap(BankTransactionRepository.TotalsByType::getTipo, Function.identity()));
		BigDecimal credit = total(byType.get("credito"));
		BigDecimal debit = total(byType.get("debito"));
		long count = byType.values().stream().mapToLong(BankTransactionRepository.TotalsByType::getCantidad).sum();
		return new TransactionSummary(count, credit, debit, credit.subtract(debit));
	}

	public ProcessingStats stats() {
		Map<String, Long> rejectedByFile = this.rejected.countByFile()
			.stream()
			.collect(Collectors.toMap(RejectedRowRepository.CountByFile::getFile,
					RejectedRowRepository.CountByFile::getCantidad));
		return new ProcessingStats(this.interests.count(), this.transactions.count(), this.movements.count(),
				this.rejected.count(), rejectedByFile);
	}

	private static BigDecimal total(BankTransactionRepository.TotalsByType totals) {
		return (totals != null && totals.getTotal() != null) ? totals.getTotal() : BigDecimal.ZERO;
	}

}
