package com.Lab1BDA.Backend.dto;

/**
 * DTO para la distancia que se recorre dentro de la misión
 */
public record DistanciaMisionDTO(
        Long idOrigen,
        Long idDestino,
        Double distanciaMetro
){}
