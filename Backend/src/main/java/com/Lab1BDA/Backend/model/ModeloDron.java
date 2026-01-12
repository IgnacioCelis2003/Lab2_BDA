package com.Lab1BDA.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeloDron {
    private Long idModelo;
    private String nombreModelo;
    private String fabricante;
    private Double capacidadCargaKg;
    private Integer autonomiaMinutos;
    private Double velocidadPromedioKmh;
}