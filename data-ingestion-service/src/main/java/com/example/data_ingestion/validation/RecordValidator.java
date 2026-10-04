package com.example.data_ingestion.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.example.data_ingestion.model.AnnualAccountRecord;
import com.example.data_ingestion.model.InterestRecord;
import com.example.data_ingestion.model.TransactionRecord;

import org.springframework.stereotype.Component;

import static com.example.data_ingestion.validation.FieldParsers.isBlank;
import static com.example.data_ingestion.validation.FieldParsers.normalizeCategory;
import static com.example.data_ingestion.validation.FieldParsers.parseDate;
import static com.example.data_ingestion.validation.FieldParsers.parseDecimal;
import static com.example.data_ingestion.validation.FieldParsers.parseLong;

/**
 * Validation and transformation rules applied to every legacy CSV row before it is
 * published to Kafka. Each method collects all the problems of a row so the rejected
 * record explains every reason at once.
 */
@Component
public class RecordValidator {

	static final Set<String> INTEREST_TYPES = Set.of("ahorro", "prestamo", "hipoteca");

	static final Set<String> TRANSACTION_TYPES = Set.of("credito", "debito");

	static final Set<String> MOVEMENT_TYPES = Set.of("deposito", "retiro", "compra", "pago");

	static final Set<String> UNKNOWN_NAMES = Set.of("unknown", "desconocido", "n/a", "null");

	static final int MAX_AGE = 120;

	static final int MIN_AGE = 18;

	public ValidationResult<InterestRecord> validateInterest(String sourceId, Map<String, String> row) {
		List<String> errors = new ArrayList<>();
		Integer cuentaId = accountId(row.get("cuenta_id"), errors);

		String nombre = row.get("nombre");
		if (isBlank(nombre) || UNKNOWN_NAMES.contains(normalizeCategory(nombre))) {
			errors.add("nombre: cliente no identificado ('" + nullToEmpty(nombre) + "')");
		}

		Optional<BigDecimal> saldo = parseDecimal(row.get("saldo"));
		if (saldo.isEmpty()) {
			errors.add("saldo: vacio o no numerico");
		}
		else if (saldo.get().signum() < 0) {
			errors.add("saldo: negativo (" + saldo.get() + ")");
		}

		Integer edad = null;
		String rawEdad = row.get("edad");
		if (!isBlank(rawEdad)) {
			Optional<Long> parsed = parseLong(rawEdad);
			if (parsed.isEmpty()) {
				errors.add("edad: no numerica ('" + rawEdad + "')");
			}
			else if (parsed.get() < MIN_AGE || parsed.get() > MAX_AGE) {
				errors.add("edad: fuera de rango " + MIN_AGE + "-" + MAX_AGE + " (" + parsed.get() + ")");
			}
			else {
				edad = parsed.get().intValue();
			}
		}

		String tipo = normalizeCategory(row.get("tipo"));
		if (!INTEREST_TYPES.contains(tipo)) {
			errors.add("tipo: desconocido ('" + nullToEmpty(row.get("tipo")) + "')");
		}

		if (!errors.isEmpty()) {
			return new ValidationResult.Invalid<>(errors);
		}
		return new ValidationResult.Valid<>(new InterestRecord(sourceId, cuentaId, nombre.trim(), saldo.get(), edad, tipo));
	}

	public ValidationResult<TransactionRecord> validateTransaction(String sourceId, Map<String, String> row) {
		List<String> errors = new ArrayList<>();
		Optional<Long> id = parseLong(row.get("id"));
		if (id.isEmpty() || id.get() <= 0) {
			errors.add("id: vacio o invalido ('" + nullToEmpty(row.get("id")) + "')");
		}
		LocalDate fecha = date(row.get("fecha"), errors);
		BigDecimal monto = positiveAmount(row.get("monto"), errors);
		String tipo = normalizeCategory(row.get("tipo"));
		if (!TRANSACTION_TYPES.contains(tipo)) {
			errors.add("tipo: desconocido ('" + nullToEmpty(row.get("tipo")) + "')");
		}

		if (!errors.isEmpty()) {
			return new ValidationResult.Invalid<>(errors);
		}
		return new ValidationResult.Valid<>(new TransactionRecord(sourceId, id.get(), fecha, monto, tipo));
	}

	public ValidationResult<AnnualAccountRecord> validateAnnualAccount(String sourceId, Map<String, String> row) {
		List<String> errors = new ArrayList<>();
		Integer cuentaId = accountId(row.get("cuenta_id"), errors);
		LocalDate fecha = date(row.get("fecha"), errors);
		String transaccion = normalizeCategory(row.get("transaccion"));
		if (!MOVEMENT_TYPES.contains(transaccion)) {
			errors.add("transaccion: desconocida ('" + nullToEmpty(row.get("transaccion")) + "')");
		}
		BigDecimal monto = positiveAmount(row.get("monto"), errors);
		String descripcion = isBlank(row.get("descripcion")) ? null : row.get("descripcion").trim();

		if (!errors.isEmpty()) {
			return new ValidationResult.Invalid<>(errors);
		}
		return new ValidationResult.Valid<>(
				new AnnualAccountRecord(sourceId, cuentaId, fecha, transaccion, monto, descripcion));
	}

	private static Integer accountId(String raw, List<String> errors) {
		Optional<Long> id = parseLong(raw);
		if (id.isEmpty() || id.get() <= 0 || id.get() > Integer.MAX_VALUE) {
			errors.add("cuenta_id: vacio o invalido ('" + nullToEmpty(raw) + "')");
			return null;
		}
		return id.get().intValue();
	}

	private static LocalDate date(String raw, List<String> errors) {
		Optional<LocalDate> fecha = parseDate(raw);
		if (fecha.isEmpty()) {
			errors.add("fecha: vacia o invalida ('" + nullToEmpty(raw) + "')");
			return null;
		}
		return fecha.get();
	}

	/**
	 * Amounts must be strictly positive: the direction of the money is given by the
	 * type column, so a negative or zero amount is inconsistent legacy data.
	 */
	private static BigDecimal positiveAmount(String raw, List<String> errors) {
		Optional<BigDecimal> monto = parseDecimal(raw);
		if (monto.isEmpty()) {
			errors.add("monto: vacio o no numerico");
			return null;
		}
		if (monto.get().signum() <= 0) {
			errors.add("monto: debe ser positivo (" + monto.get() + ")");
			return null;
		}
		return monto.get();
	}

	private static String nullToEmpty(String value) {
		return (value != null) ? value : "";
	}

}
