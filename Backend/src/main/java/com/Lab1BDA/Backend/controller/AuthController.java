package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.dto.AuthResponse;
import com.Lab1BDA.Backend.dto.LoginRequest;
import com.Lab1BDA.Backend.dto.RegisterRequest;
import com.Lab1BDA.Backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    // 1. Inyectamos nuestro nuevo AuthService
    @Autowired
    private AuthService authService;

    // 2. Aquí implementamos tu sugerencia de tipo de respuesta estricto
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {

        // 3. El controlador solo delega la lógica al servicio
        AuthResponse authResponse = authService.login(loginRequest);

        // 4. Y devuelve el resultado
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     * Es público, igual que /login.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            // Devolvemos 201 Created si fue exitoso
            return ResponseEntity.status(HttpStatus.CREATED).body("¡Usuario registrado exitosamente!");
        } catch (IllegalArgumentException e) {
            // Devolvemos 400 Bad Request si el email ya existe
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}