package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.model.ModeloDron;
import com.Lab1BDA.Backend.repository.mappers.ModeloDronRowMapper;
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
public class ModeloDronRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<ModeloDron> findAll() {
        String sql = "SELECT id_modelo, nombre_modelo, fabricante, capacidad_carga_kg, autonomia_minutos, velocidad_promedio_kmh FROM modelos_dron";
        return jdbcTemplate.query(sql, new ModeloDronRowMapper());
    }

    public Optional<ModeloDron> findById(Long id) {
        String sql = "SELECT id_modelo, nombre_modelo, fabricante, capacidad_carga_kg, autonomia_minutos, velocidad_promedio_kmh FROM modelos_dron WHERE id_modelo = ?";
        List<ModeloDron> list = jdbcTemplate.query(sql, new ModeloDronRowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public ModeloDron save(ModeloDron modelo) {
        String sql = "INSERT INTO modelos_dron (nombre_modelo, fabricante, capacidad_carga_kg, autonomia_minutos, velocidad_promedio_kmh) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, modelo.getNombreModelo());
            ps.setString(2, modelo.getFabricante());
            if (modelo.getCapacidadCargaKg() != null) ps.setDouble(3, modelo.getCapacidadCargaKg()); else ps.setNull(3, java.sql.Types.DOUBLE);
            if (modelo.getAutonomiaMinutos() != null) ps.setInt(4, modelo.getAutonomiaMinutos()); else ps.setNull(4, java.sql.Types.INTEGER);
            if (modelo.getVelocidadPromedioKmh() != null) ps.setDouble(5, modelo.getVelocidadPromedioKmh()); else ps.setNull(5, java.sql.Types.DOUBLE);
            return ps;
        }, keyHolder);

        // Try to obtain generated id
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id_modelo")) {
            modelo.setIdModelo(((Number) keyHolder.getKeys().get("id_modelo")).longValue());
        } else if (keyHolder.getKey() != null) {
            modelo.setIdModelo(keyHolder.getKey().longValue());
        }

        return modelo;
    }

    public ModeloDron update(ModeloDron modelo) {
        String sql = "UPDATE modelos_dron SET nombre_modelo = ?, fabricante = ?, capacidad_carga_kg = ?, autonomia_minutos = ?, velocidad_promedio_kmh = ? WHERE id_modelo = ?";
        jdbcTemplate.update(sql,
                modelo.getNombreModelo(),
                modelo.getFabricante(),
                modelo.getCapacidadCargaKg(),
                modelo.getAutonomiaMinutos(),
                modelo.getVelocidadPromedioKmh(),
                modelo.getIdModelo());
        return modelo;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM modelos_dron WHERE id_modelo = ?";
        jdbcTemplate.update(sql, id);
    }
}

