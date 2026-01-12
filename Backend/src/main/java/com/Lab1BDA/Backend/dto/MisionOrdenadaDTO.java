package com.Lab1BDA.Backend.dto;

/**
 * DTO para guardar una mision ordenada según optimización
 */
public record MisionOrdenadaDTO(
        int orden,              // Número de prioridad para las misiones
        Long idMision,
        String nombreTipo,
        String coordenadasWKT   // Para mostrar la ruta en el mapa
){}
