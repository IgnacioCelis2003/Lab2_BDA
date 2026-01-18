package com.Lab1BDA.Backend.dto;

import org.locationtech.jts.geom.LineString;

/**
 * DTO para guardar una mision ordenada según optimización
 */
public record MisionOrdenadaDTO(
        int orden,              // Número de prioridad para las misiones
        Long idMision,
        String nombreTipo,
        LineString coordenadasWKT   // Para mostrar la ruta en el mapa
){}
