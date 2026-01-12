package com.Lab1BDA.Backend.dto;

import java.time.LocalDateTime;

/**
 * DTO para mostrar ubicación en tiempo real.
 */
public record UbicacionDTO(
    Long idMision,
    LocalDateTime timestamp,
    double latitud,
    double longitud,
    Double nivelBateriaPorcentaje
) {}
