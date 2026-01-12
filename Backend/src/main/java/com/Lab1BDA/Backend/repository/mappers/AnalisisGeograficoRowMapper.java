package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.AnalisisGeograficoDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnalisisGeograficoRowMapper implements RowMapper<AnalisisGeograficoDTO> {

    @Override
    public AnalisisGeograficoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AnalisisGeograficoDTO(
                rs.getLong("id_dron"),
                rs.getDouble("distancia_minima_metros")
        );
    }
}