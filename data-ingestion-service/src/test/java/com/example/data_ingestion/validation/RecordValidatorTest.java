package com.example.data_ingestion.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.example.data_ingestion.model.AnnualAccountRecord;
import com.example.data_ingestion.model.InterestRecord;
import com.example.data_ingestion.model.TransactionRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RecordValidatorTest {

	private final RecordValidator validator = new RecordValidator();

	@Test
	void acceptsCleanInterestAndAllowsMissingAge() {
		ValidationResult<InterestRecord> result = this.validator.validateInterest("intereses.csv:2",
				row("cuenta_id", "137", "nombre", "Bob Johnson", "saldo", "7000", "edad", "", "tipo", "Prestamo"));
		assertThat(result).isEqualTo(new ValidationResult.Valid<>(
				new InterestRecord("intereses.csv:2", 137, "Bob Johnson", new BigDecimal("7000"), null, "prestamo")));
	}

	@Test
	void rejectsInterestWithEveryDirtyFieldListed() {
		ValidationResult<InterestRecord> result = this.validator.validateInterest("intereses.csv:3",
				row("cuenta_id", "114", "nombre", "Unknown", "saldo", "", "edad", "150", "tipo", "-1"));
		assertThat(result).isInstanceOfSatisfying(ValidationResult.Invalid.class,
				(invalid) -> assertThat(invalid.reasons()).hasSize(4)
					.anyMatch((r) -> r.toString().startsWith("nombre:"))
					.anyMatch((r) -> r.toString().startsWith("saldo:"))
					.anyMatch((r) -> r.toString().startsWith("edad:"))
					.anyMatch((r) -> r.toString().startsWith("tipo:")));
	}

	@ParameterizedTest
	@CsvSource({ "2024-06-30, 2024-06-30", "03-04-2024, 2024-04-03", "04/05/2024, 2024-05-04",
			"2024/10/15, 2024-10-15" })
	void normalizesEveryLegacyDateLayout(String raw, LocalDate expected) {
		ValidationResult<TransactionRecord> result = this.validator.validateTransaction("t:1",
				row("id", "1", "fecha", raw, "monto", "800", "tipo", "credito"));
		assertThat(result).isInstanceOfSatisfying(ValidationResult.Valid.class,
				(valid) -> assertThat(((TransactionRecord) valid.value()).fecha()).isEqualTo(expected));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "2024-13-01|800|debito|fecha", "2024-04-09|800|invalid|tipo",
			"2024-04-09|800|desconocido|tipo", "2024-04-09||credito|monto", "2024-04-09|-200|credito|monto",
			"2024-04-09|0|credito|monto", "31/02/2024|800|credito|fecha" })
	void rejectsDirtyTransactions(String fecha, String monto, String tipo, String failingField) {
		ValidationResult<TransactionRecord> result = this.validator.validateTransaction("t:1",
				row("id", "7", "fecha", fecha, "monto", monto, "tipo", tipo));
		assertThat(result).isInstanceOfSatisfying(ValidationResult.Invalid.class,
				(invalid) -> assertThat(invalid.reasons()).singleElement()
					.asString()
					.startsWith(failingField + ":"));
	}

	@Test
	void normalizesAccentedMovementTypeAndEmptyDescription() {
		ValidationResult<AnnualAccountRecord> result = this.validator.validateAnnualAccount("c:2",
				row("cuenta_id", "110", "fecha", "24-07-2024", "transaccion", "Depósito", "monto", "1500",
						"descripcion", ""));
		assertThat(result).isEqualTo(new ValidationResult.Valid<>(new AnnualAccountRecord("c:2", 110,
				LocalDate.of(2024, 7, 24), "deposito", new BigDecimal("1500"), null)));
	}

	@Test
	void rejectsNegativeMovementAmount() {
		ValidationResult<AnnualAccountRecord> result = this.validator.validateAnnualAccount("c:3",
				row("cuenta_id", "106", "fecha", "12/02/2024", "transaccion", "deposito", "monto", "-100",
						"descripcion", "Ingreso navideño"));
		assertThat(result).isInstanceOf(ValidationResult.Invalid.class);
	}

	private static Map<String, String> row(String... keyValues) {
		Map<String, String> row = new HashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			row.put(keyValues[i], keyValues[i + 1]);
		}
		return row;
	}

}
