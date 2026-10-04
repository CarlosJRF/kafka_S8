package com.example.data_processor.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Audit trail of a legacy row rejected by the ingestion validation rules.
 */
@Entity
@Table(name = "rejected_rows", indexes = @Index(name = "idx_rejected_file", columnList = "file"))
public class RejectedRow {

	@Id
	private String sourceId;

	private String file;

	private long line;

	@Column(length = 2000)
	private String raw;

	@Column(length = 2000)
	private String reasons;

	private Instant processedAt;

	protected RejectedRow() {
	}

	public RejectedRow(String sourceId, String file, long line, String raw, String reasons) {
		this.sourceId = sourceId;
		this.file = file;
		this.line = line;
		this.raw = raw;
		this.reasons = reasons;
		this.processedAt = Instant.now();
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public String getFile() {
		return this.file;
	}

	public long getLine() {
		return this.line;
	}

	public String getRaw() {
		return this.raw;
	}

	public String getReasons() {
		return this.reasons;
	}

	public Instant getProcessedAt() {
		return this.processedAt;
	}

}
