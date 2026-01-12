package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DistanciaMisionDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DistanciaMisionRowMapper implements RowMapper<DistanciaMisionDTO> {
    @Override
    public DistanciaMisionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DistanciaMisionDTO(
                rs.getLong("id_origen"),
                rs.getLong("id_destino"),
                rs.getDouble("distancia_metros")
        );
    }
}
