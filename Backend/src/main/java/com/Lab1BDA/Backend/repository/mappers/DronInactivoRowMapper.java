package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DronInactivoDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class DronInactivoRowMapper implements RowMapper<DronInactivoDTO> {

    @Override
    public DronInactivoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DronInactivoDTO(
                rs.getLong("id_dron"),
                rs.getString("nombre_modelo"),
                rs.getString("estado"),
                rs.getObject("ultima_mision", LocalDate.class) // Maneja 'null' automáticamente
        );
    }
}