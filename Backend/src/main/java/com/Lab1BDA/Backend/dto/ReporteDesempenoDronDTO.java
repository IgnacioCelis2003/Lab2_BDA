package com.Lab1BDA.Backend.dto;

public record ReporteDesempenoDronDTO(
        Long idDron,
        String modelo,
        Integer misionesCompletadas,
        Integer misionesFallidas,
        Double horasVueloTotal
) {
}
