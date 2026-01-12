package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.BateriaConsumoDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BateriaConsumoRowMapper implements RowMapper<BateriaConsumoDTO> {

    @Override
    public BateriaConsumoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new BateriaConsumoDTO(
                rs.getLong("id_mision"),
                rs.getDouble("duracion_minutos"),
                rs.getDouble("consumo_bateria")
        );
    }
}