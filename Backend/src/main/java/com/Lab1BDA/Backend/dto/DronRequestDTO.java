package com.Lab1BDA.Backend.dto;

/**
 * Record DTO para manejar las peticiones de creación y actualización de un Dron.
 * Solo incluimos los campos que el cliente puede especificar.
 */
public record DronRequestDTO(
        Long idModelo,
        String estado // Ej: 'Disponible', 'En Mantenimiento'
) {}