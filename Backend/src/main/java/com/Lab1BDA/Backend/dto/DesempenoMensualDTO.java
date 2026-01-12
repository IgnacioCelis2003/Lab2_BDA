package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #5: Análisis de Desempeño Mensual.
 */
public record DesempenoMensualDTO(
        String mes, // Formato 'YYYY-MM'
        Double promedioSemanal,
        Double diferenciaMesAnterior
) {}