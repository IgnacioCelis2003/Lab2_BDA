package com.Lab1BDA.Backend.dto;

import java.time.LocalDateTime;

/**
 * DTO para recibir un único punto de telemetría desde un dron.
 */
public record RegistroVueloRequestDTO(
        Long idMision,
        LocalDateTime timestamp, // Cuándo se tomó la lectura
        String coordenadasWKT, // Ej: "POINT(-70.648 33.437)"
        Double altitudMsnm,
        Double velocidadKmh,
        Double nivelBateriaPorcentaje
) {}