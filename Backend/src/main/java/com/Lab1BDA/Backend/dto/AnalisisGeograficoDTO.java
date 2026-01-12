package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #9: Análisis Geográfico de Puntos de Interés.
 */
public record AnalisisGeograficoDTO(
        Long idDron,
        Double distanciaMinimaMetros
) {}