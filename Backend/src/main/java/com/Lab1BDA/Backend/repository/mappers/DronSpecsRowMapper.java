package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DronSpecsDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DronSpecsRowMapper implements RowMapper<DronSpecsDTO> {
    @Override
    public DronSpecsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DronSpecsDTO(
                rs.getLong("id_dron"),
                rs.getString("nombre_modelo"),
                rs.getInt("autonomia_minutos"),
                rs.getDouble("capacidad_carga_kg"),
                rs.getDouble("velocidad_promedio_kmh")
        );
    }
}