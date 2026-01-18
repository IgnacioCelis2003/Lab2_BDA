package com.Lab1BDA.Backend.dto;

import java.time.LocalDateTime;

public record VelocidadCalculadaDTO(
        LocalDateTime timestamp,      // Cuándo ocurrió
        Double distanciaRecorrida,    // Metros recorridos desde el punto anterior
        Double segundosTranscurridos, // Segundos desde el punto anterior
        Double velocidadCalculada     // Km/h calculado (Física)
) {}