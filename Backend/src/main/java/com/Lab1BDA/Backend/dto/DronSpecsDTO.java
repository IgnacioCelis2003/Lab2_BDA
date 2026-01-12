package com.Lab1BDA.Backend.dto;

/**
 * DTO para guardar solo las especificaciones del dron con respecto a la asignación de misiones
 */
public record DronSpecsDTO(
        Long idDron,
        String nombreModelo,
        Integer autonomiaMinutos,
        Double capacidadCargaKg,
        Double velocidadKmh
){}
