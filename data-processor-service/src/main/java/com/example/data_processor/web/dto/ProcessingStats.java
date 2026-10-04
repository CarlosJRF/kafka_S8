package com.example.data_processor.web.dto;

import java.util.Map;

public record ProcessingStats(long productosConInteres, long transacciones, long movimientosAnuales,
		long filasRechazadas, Map<String, Long> rechazadasPorArchivo) {
}
