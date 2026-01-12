package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DesempenoMensualDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DesempenoMensualRowMapper implements RowMapper<DesempenoMensualDTO> {

    @Override
    public DesempenoMensualDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DesempenoMensualDTO(
                rs.getString("mes"),
                rs.getDouble("promedio_semanal"),
                rs.getDouble("diferencia_mes_anterior")
        );
    }
}