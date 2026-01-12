package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.model.Usuario;
import com.Lab1BDA.Backend.repository.mappers.UsuarioRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Guarda un nuevo usuario en la base de datos.
     * @param usuario El objeto Usuario a guardar (con la contraseña ya hasheada).
     * @return El objeto Usuario guardado (con el ID generado por la BD).
     */
    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, email, contrasena_hash, rol) " +
                "VALUES (?, ?, ?, CAST(? AS rol_usuario))";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getContrasenaHash());
            ps.setString(4, usuario.getRol());
            return ps;
        }, keyHolder);

        // Obtenemos el ID generado y lo asignamos al objeto usuario
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id_usuario")) {
            usuario.setIdUsuario(((Number) keyHolder.getKeys().get("id_usuario")).longValue());
        }

        return usuario;
    }
    /**
     * Busca un usuario por su email.
     * @param email El email del usuario a buscar.
     * @return Un Optional que contiene el Usuario si se encuentra, o vacío si no.
     */

    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT id_usuario, nombre, email, contrasena_hash, rol FROM usuarios WHERE email = ?";
        List<Usuario> usuarios = jdbcTemplate.query(sql, new UsuarioRowMapper(), email);
        return usuarios.isEmpty() ? Optional.empty() : Optional.of(usuarios.get(0));
    }
}