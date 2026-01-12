package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #2: Drones con Fallos Recurrentes.
 */
public record DronFalloDTO(
        Long idDron,
        Long totalMisionesFallidas
) {}