package com.Lab1BDA.Backend.dto;

import java.util.List;

/**
 * DTO para el reporte de la ruta optima
 */
public record RutaOptimaResponseDTO(
        List<RutaAsignadaDTO> asignaciones,     // Lista de la ruta optima asignada al dron
        List<Long> misionesSinAsignar,          // ID de las misiones que aún no han sido asignadas
        String mensaje
){}
