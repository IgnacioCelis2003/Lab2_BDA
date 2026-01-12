package com.Lab1BDA.Backend.dto;

import java.time.LocalDate;

/**
 * DTO para el reporte del Requisito #8: Drones Inactivos.
 * Contiene los campos específicos solicitados en el enunciado.
 */
public record DronInactivoDTO(
        Long idDron,
        String nombreModelo,
        String estado,
        LocalDate ultimaMision // La fecha de la última misión (puede ser null)
) {}