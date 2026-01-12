package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.Usuario;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Esta clase mapea una fila del ResultSet de la tabla 'usuarios'
 * a un objeto del modelo 'Usuario'.
 */
public class UsuarioRowMapper implements RowMapper<Usuario> {

    @Override
    public Usuario mapRow(ResultSet rs, int rowNum) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getLong("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));
        usuario.setContrasenaHash(rs.getString("contrasena_hash"));
        usuario.setRol(rs.getString("rol"));
        return usuario;
    }
}