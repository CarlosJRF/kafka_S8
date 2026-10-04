package com.example.data_processor.web;

import java.util.List;

import com.example.data_processor.domain.AccountMovementRepository;
import com.example.data_processor.domain.RejectedRow;
import com.example.data_processor.domain.RejectedRowRepository;
import com.example.data_processor.service.QueryService;
import com.example.data_processor.web.dto.AccountSummary;
import com.example.data_processor.web.dto.ProcessingStats;
import com.example.data_processor.web.dto.TransactionSummary;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataQueryController {

	private static final int MAX_REJECTED_PAGE = 200;

	private final QueryService queryService;

	private final RejectedRowRepository rejectedRows;

	public DataQueryController(QueryService queryService, RejectedRowRepository rejectedRows) {
		this.queryService = queryService;
		this.rejectedRows = rejectedRows;
	}

	@GetMapping("/api/accounts")
	public List<AccountMovementRepository.AccountBalance> accounts() {
		return this.queryService.balances();
	}

	@GetMapping("/api/accounts/{cuentaId}/summary")
	public ResponseEntity<AccountSummary> accountSummary(@PathVariable int cuentaId) {
		return ResponseEntity.of(this.queryService.accountSummary(cuentaId));
	}

	@GetMapping("/api/transactions/summary")
	public TransactionSummary transactionSummary() {
		return this.queryService.transactionSummary();
	}

	@GetMapping("/api/stats")
	public ProcessingStats stats() {
		return this.queryService.stats();
	}

	@GetMapping("/api/stats/rejected")
	public List<RejectedRowView> rejected(@RequestParam String file, @RequestParam(defaultValue = "20") int limit) {
		int size = Math.clamp(limit, 1, MAX_REJECTED_PAGE);
		return this.rejectedRows.findByFileOrderByLine(file, PageRequest.of(0, size))
			.stream()
			.map(RejectedRowView::of)
			.toList();
	}

	public record RejectedRowView(String sourceId, long line, String raw, String reasons) {

		static RejectedRowView of(RejectedRow row) {
			return new RejectedRowView(row.getSourceId(), row.getLine(), row.getRaw(), row.getReasons());
		}

	}

}
