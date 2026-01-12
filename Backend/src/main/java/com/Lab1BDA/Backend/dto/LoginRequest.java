package com.Lab1BDA.Backend.dto;

/**
 * DTO para la solicitud de inicio de sesión
 */
public record LoginRequest(String email, String password) {
}