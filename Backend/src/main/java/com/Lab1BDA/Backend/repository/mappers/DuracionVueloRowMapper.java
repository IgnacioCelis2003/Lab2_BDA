package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DuracionVueloDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DuracionVueloRowMapper implements RowMapper<DuracionVueloDTO> {

    @Override
    public DuracionVueloDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DuracionVueloDTO(
                rs.getString("nombre_modelo"),
                // getDouble maneja la conversión numérica
                rs.getDouble("tiempo_total_horas")
        );
    }
}