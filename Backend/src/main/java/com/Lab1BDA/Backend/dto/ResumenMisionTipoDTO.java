package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #10: Resumen de Misiones por Tipo.
 * Refleja las columnas de la vista materializada 'resumen_misiones_completadas'.
 */
public record ResumenMisionTipoDTO(
        String nombreTipo,
        Long cantidadTotal,
        Double promedioHoras
) {}