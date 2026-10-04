package com.example.data_processor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.data_processor.config.ProcessorProperties;
import com.example.data_processor.domain.AccountMovement;
import com.example.data_processor.domain.AccountMovementRepository;
import com.example.data_processor.domain.BankTransaction;
import com.example.data_processor.domain.BankTransactionRepository;
import com.example.data_processor.domain.InterestProduct;
import com.example.data_processor.domain.InterestProductRepository;
import com.example.data_processor.domain.RejectedRow;
import com.example.data_processor.domain.RejectedRowRepository;
import com.example.data_processor.event.AnnualAccountEvent;
import com.example.data_processor.event.InterestEvent;
import com.example.data_processor.event.RejectedEvent;
import com.example.data_processor.event.TransactionEvent;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns Kafka events into rows. Every entity is keyed by the event {@code sourceId}, so
 * re-processing the same event (redelivery or a second ingestion run) updates the row
 * instead of duplicating it.
 */
@Service
public class EventProcessingService {

	private final ProcessorProperties properties;

	private final InterestProductRepository interests;

	private final BankTransactionRepository transactions;

	private final AccountMovementRepository movements;

	private final RejectedRowRepository rejected;

	private final JsonMapper jsonMapper;

	public EventProcessingService(ProcessorProperties properties, InterestProductRepository interests,
			BankTransactionRepository transactions, AccountMovementRepository movements, RejectedRowRepository rejected,
			JsonMapper jsonMapper) {
		this.properties = properties;
		this.interests = interests;
		this.transactions = transactions;
		this.movements = movements;
		this.rejected = rejected;
		this.jsonMapper = jsonMapper;
	}

	@Transactional
	public InterestProduct process(InterestEvent event) {
		BigDecimal rate = this.properties.rateFor(event.tipo());
		BigDecimal interest = event.saldo().multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
		return this.interests.save(new InterestProduct(event.sourceId(), event.cuentaId(), event.nombre(),
				event.saldo(), event.edad(), event.tipo(), rate, interest));
	}

	@Transactional
	public BankTransaction process(TransactionEvent event) {
		return this.transactions
			.save(new BankTransaction(event.sourceId(), event.id(), event.fecha(), event.monto(), event.tipo()));
	}

	@Transactional
	public AccountMovement process(AnnualAccountEvent event) {
		return this.movements.save(new AccountMovement(event.sourceId(), event.cuentaId(), event.fecha(),
				event.transaccion(), event.monto(), event.descripcion()));
	}

	@Transactional
	public RejectedRow process(RejectedEvent event) {
		return this.rejected.save(new RejectedRow(event.sourceId(), event.file(), event.line(),
				this.jsonMapper.writeValueAsString(event.raw()), String.join("; ", event.reasons())));
	}

}
