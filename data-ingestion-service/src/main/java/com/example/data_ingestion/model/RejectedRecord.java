package com.example.data_ingestion.model;

import java.util.List;
import java.util.Map;

/**
 * A row that failed validation, published to the rejected-records topic for auditing.
 */
public record RejectedRecord(String sourceId, String file, long line, Map<String, String> raw, List<String> reasons) {
}
