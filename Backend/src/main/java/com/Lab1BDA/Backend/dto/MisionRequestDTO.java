package com.Lab1BDA.Backend.dto;

import java.time.LocalDateTime;

/**
 * Record DTO para manejar las peticiones de creación y actualización de Misiones.
 * Usamos WKT (Well-Known Text) para la ruta.
 */
public record MisionRequestDTO(
        Long idTipoMision,
        Long idDronAsignado, // Puede ser null al crear
        LocalDateTime fechaInicioPlanificada,
        LocalDateTime fechaFinPlanificada,
        String estado, // Ej: 'Pendiente', 'Completada'
        String rutaWKT  // La ruta en formato WKT, ej: "LINESTRING(-70.6 -33.4, -70.5 -33.3)"
) {}