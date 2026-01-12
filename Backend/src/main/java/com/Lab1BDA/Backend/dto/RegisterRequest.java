package com.Lab1BDA.Backend.dto;

/**
 * DTO para la petición de registro de un nuevo usuario.
 */
public record RegisterRequest(
        String nombre,
        String email,
        String password
) {}