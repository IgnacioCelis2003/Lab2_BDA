package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.ModeloDron;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ModeloDronRowMapper implements RowMapper<ModeloDron> {
    @Override
    public ModeloDron mapRow(ResultSet rs, int rowNum) throws SQLException {
        ModeloDron m = new ModeloDron();
        m.setIdModelo(rs.getLong("id_modelo"));
        m.setNombreModelo(rs.getString("nombre_modelo"));
        m.setFabricante(rs.getString("fabricante"));

        // capacidad_carga_kg may be null
        Double capacidad = rs.getObject("capacidad_carga_kg") != null ? rs.getDouble("capacidad_carga_kg") : null;
        m.setCapacidadCargaKg(capacidad);

        Integer autonomia = rs.getObject("autonomia_minutos") != null ? rs.getInt("autonomia_minutos") : null;
        m.setAutonomiaMinutos(autonomia);

        Double velocidad_promedio = rs.getObject("velocidad_promedio_kmh") != null ? rs.getDouble("velocidad_promedio_kmh") : null;
        m.setVelocidadPromedioKmh(velocidad_promedio);

        return m;
    }
}

