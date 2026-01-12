package com.Lab1BDA.Backend.config;

import com.Lab1BDA.Backend.security.JwtRequestFilter;
import com.Lab1BDA.Backend.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http)
            throws Exception {

        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // API stateless: sin CSRF
                .csrf(csrf -> csrf.disable())

                // CORS por defecto
                .cors(Customizer.withDefaults())

                // Sin sesiones (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Responder 401 cuando falte o sea inválido el token
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                (req, res, e) ->
                                        res.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )
                )

                // Reglas de acceso
                .authorizeHttpRequests(authz -> authz
                        // Público
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        /*
                        // Público (desarrollo)
                        .requestMatchers("/api/modelos/**").permitAll()
                        .requestMatchers("/api/drones/**").permitAll()
                        .requestMatchers("/api/misiones/**").permitAll()
                        .requestMatchers("/api/tipos-mision/**").permitAll()

                         */

                        // Todo lo demás requiere JWT
                        .anyRequest().authenticated()
                )

                // Filtro JWT
                .addFilterBefore(
                        jwtRequestFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
