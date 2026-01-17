package com.Lab1BDA.Backend.dto;

public record ProximidadDTO(
        Long misionId,
        Long poiId,
        Double distanciaMinima3DMetros
) {}