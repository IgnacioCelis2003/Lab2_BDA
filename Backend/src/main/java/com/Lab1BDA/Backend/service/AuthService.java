package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.dto.AuthResponse;
import com.Lab1BDA.Backend.dto.LoginRequest;
import com.Lab1BDA.Backend.dto.RegisterRequest;
import com.Lab1BDA.Backend.model.Usuario;
import com.Lab1BDA.Backend.repository.UserRepository;
import com.Lab1BDA.Backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; // Importamos el Hasher
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;





    /**
     * Registra un nuevo usuario en el sistema.
     * @param registerRequest DTO con nombre, email y contraseña.
     */
    public void register(RegisterRequest registerRequest) {
        // 1. Verificar si el email ya existe
        if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
            // Sería ideal tener una excepción personalizada, pero por ahora usamos esta
            throw new IllegalArgumentException("Error: El email ya está en uso.");
        }

        // 2. Crear el nuevo objeto Usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registerRequest.nombre());
        usuario.setEmail(registerRequest.email());

        // 3. ¡IMPORTANTE! Hashear la contraseña antes de guardarla
        usuario.setContrasenaHash(passwordEncoder.encode(registerRequest.password()));

        // 4. Asignar rol por defecto
        usuario.setRol("Operador"); // Todos los nuevos registros son 'Operador'

        // 5. Guardar el usuario en la base de datos
        userRepository.save(usuario);
    }




    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return new AuthResponse(jwt);
    }
}