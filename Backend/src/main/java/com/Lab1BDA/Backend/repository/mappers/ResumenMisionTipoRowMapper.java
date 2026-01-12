package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.ResumenMisionTipoDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResumenMisionTipoRowMapper implements RowMapper<ResumenMisionTipoDTO> {

    @Override
    public ResumenMisionTipoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ResumenMisionTipoDTO(
                rs.getString("nombre_tipo"),
                rs.getLong("cantidad_total"),
                rs.getDouble("promedio_horas")
        );
    }
}