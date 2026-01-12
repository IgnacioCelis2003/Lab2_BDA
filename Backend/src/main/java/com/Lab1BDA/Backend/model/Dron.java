package com.Lab1BDA.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dron {
    private Long idDron;
    private Long idModelo;
    private String estado;
}