package com.Lab1BDA.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long idUsuario;
    private String nombre;
    private String email;
    private String contrasenaHash;
    private String rol;

}

