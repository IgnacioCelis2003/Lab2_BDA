package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #4: Patrones de Consumo de Batería.
 */
public record BateriaConsumoDTO(
        Long idMision,
        Double duracionMinutos,
        Double consumoBateria // Batería consumida (ej: 20.5%)
) {}