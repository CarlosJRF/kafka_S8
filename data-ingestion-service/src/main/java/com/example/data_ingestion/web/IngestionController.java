package com.example.data_ingestion.web;

import com.example.data_ingestion.service.IngestionInProgressException;
import com.example.data_ingestion.service.IngestionReport;
import com.example.data_ingestion.service.IngestionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

	private final IngestionService ingestionService;

	public IngestionController(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping("/run")
	public IngestionReport run() {
		return this.ingestionService.ingestAll();
	}

	@GetMapping("/last-report")
	public ResponseEntity<IngestionReport> lastReport() {
		return ResponseEntity.of(this.ingestionService.lastReport());
	}

	@ExceptionHandler(IngestionInProgressException.class)
	ProblemDetail ingestionInProgress(IngestionInProgressException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

}
