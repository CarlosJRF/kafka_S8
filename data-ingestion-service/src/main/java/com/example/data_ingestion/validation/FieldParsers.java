package com.example.data_ingestion.validation;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lenient-input, strict-output parsers for the dirty legacy values.
 */
final class FieldParsers {

	/**
	 * Date layouts found in the legacy files. Day-first layouts follow the Spanish
	 * convention; {@link ResolverStyle#STRICT} rejects impossible dates such as month 13.
	 */
	private static final List<DateTimeFormatter> DATE_FORMATS = List.of("uuuu-MM-dd", "uuuu/MM/dd", "dd-MM-uuuu",
			"dd/MM/uuuu")
		.stream()
		.map((pattern) -> DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT))
		.toList();

	private FieldParsers() {
	}

	static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	static Optional<Long> parseLong(String value) {
		if (isBlank(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(value.trim()));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	static Optional<BigDecimal> parseDecimal(String value) {
		if (isBlank(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new BigDecimal(value.trim()));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	static Optional<LocalDate> parseDate(String value) {
		if (isBlank(value)) {
			return Optional.empty();
		}
		for (DateTimeFormatter format : DATE_FORMATS) {
			try {
				return Optional.of(LocalDate.parse(value.trim(), format));
			}
			catch (DateTimeParseException ex) {
				// try the next layout
			}
		}
		return Optional.empty();
	}

	/**
	 * Lower-cases, trims and strips accents so that {@code "Depósito"} matches {@code "deposito"}.
	 */
	static String normalizeCategory(String value) {
		if (value == null) {
			return "";
		}
		String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
		return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

}
