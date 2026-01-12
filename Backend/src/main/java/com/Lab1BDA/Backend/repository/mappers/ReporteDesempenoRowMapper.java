package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.ReporteDesempenoDronDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReporteDesempenoRowMapper implements RowMapper<ReporteDesempenoDronDTO> {
    @Override
    public ReporteDesempenoDronDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReporteDesempenoDronDTO(
                rs.getLong("id_dron"),
                rs.getString("nombre_modelo"),
                rs.getInt("total_completadas"),
                rs.getInt("total_fallidas"),
                rs.getDouble("horas_vuelo")
        );
    }
}