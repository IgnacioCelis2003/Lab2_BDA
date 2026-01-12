package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #3: Desempeño por Tipo de Misión.
 */
public record DesempenoTipoMisionDTO(
        String nombreTipoMision,
        String nombreModelo,
        Long totalMisionesCompletadas
) {}