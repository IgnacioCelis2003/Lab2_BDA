package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.model.TipoMision;
import com.Lab1BDA.Backend.repository.mappers.TipoMisionRowMapper;
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
public class TipoMisionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<TipoMision> findAll() {
        String sql = "SELECT id_tipo_mision, nombre_tipo FROM tipos_mision";
        return jdbcTemplate.query(sql, new TipoMisionRowMapper());
    }

    public Optional<TipoMision> findById(Long id) {
        String sql = "SELECT id_tipo_mision, nombre_tipo FROM tipos_mision WHERE id_tipo_mision = ?";
        List<TipoMision> list = jdbcTemplate.query(sql, new TipoMisionRowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public TipoMision save(TipoMision tipoMision) {
        String sql = "INSERT INTO tipos_mision (nombre_tipo) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tipoMision.getNombreTipo());
            return ps;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys == null) {
            throw new IllegalStateException("No se retornaron claves generadas");
        }

        Object rawId = keys.get("id_tipo_mision");
        if (rawId == null) rawId = keys.get("idTipoMision");
        if (rawId == null) rawId = keys.get("ID_TIPO_MISION");

        if (rawId == null) {
            throw new IllegalStateException("No se encontró la clave generada id_tipo_mision");
        }

        tipoMision.setIdTipoMision(((Number) rawId).longValue());
        return tipoMision;
    }


    public TipoMision update(TipoMision tipoMision) {
        String sql = "UPDATE tipos_mision SET nombre_tipo = ? WHERE id_tipo_mision = ?";
        jdbcTemplate.update(sql, tipoMision.getNombreTipo(), tipoMision.getIdTipoMision());
        return tipoMision;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM tipos_mision WHERE id_tipo_mision = ?";
        jdbcTemplate.update(sql, id);
    }
}

