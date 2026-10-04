package com.example.data_ingestion.validation;

import java.util.List;

/**
 * Outcome of validating one CSV row: either a clean record or the list of reasons it was rejected.
 */
public sealed interface ValidationResult<T> {

	record Valid<T>(T value) implements ValidationResult<T> {
	}

	record Invalid<T>(List<String> reasons) implements ValidationResult<T> {
	}

}
