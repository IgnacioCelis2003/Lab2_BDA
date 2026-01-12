package com.Lab1BDA.Backend.dto;

/**
 * DTO para el reporte del Requisito #1: Análisis de Duración de Vuelo.
 * Muestra el tiempo total de vuelo en horas por modelo de dron.
 */
public record DuracionVueloDTO(
        String nombreModelo,
        Double tiempoTotalHoras
) {}