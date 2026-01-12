package com.Lab1BDA.Backend.dto;

import java.util.List;

/**
 * DTO para guardar la ruta asignada al dron con la lista de misiones optimizada
 */
public record RutaAsignadaDTO(
        Long idDron,
        String nombreModelo,
        List<MisionOrdenadaDTO> misiones,
        Double distanciaTotalMetros,
        Double tiempoEstimadoMinutos,
        Double bateriaRestanteMinutos,
        Double capacidadRestanteKg
){}