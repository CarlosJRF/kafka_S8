package com.example.data_processor.event;

import java.util.List;
import java.util.Map;

/**
 * Payload of the {@code ingestion-rejected} topic.
 */
public record RejectedEvent(String sourceId, String file, long line, Map<String, String> raw, List<String> reasons) {
}
