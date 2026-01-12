package com.Lab1BDA.Backend.service;

// Importamos el nuevo modelo y repositorio
import com.Lab1BDA.Backend.model.Usuario;
import com.Lab1BDA.Backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
// ¡Ya no necesitamos JdbcTemplate aquí!
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional; // Usaremos Optional

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // 1. Inyectamos el Repositorio, no el JdbcTemplate
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 2. Usamos el repositorio para encontrar al usuario
        // Esto es mucho más limpio: la lógica de SQL está en otro lado.
        Optional<Usuario> usuarioOpt = userRepository.findByEmail(email);

        // 3. Verificamos si el Optional está vacío
        if (usuarioOpt.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
        }

        // 4. Obtenemos el objeto Usuario de nuestro modelo
        Usuario usuario = usuarioOpt.get();

        // 5. Convertimos nuestro Usuario (modelo) al User (Spring Security)
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()));

        return new User(
                usuario.getEmail(),
                usuario.getContrasenaHash(),
                authorities
        );
    }
}