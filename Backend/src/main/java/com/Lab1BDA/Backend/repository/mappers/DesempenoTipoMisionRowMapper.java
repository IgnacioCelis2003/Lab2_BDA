package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DesempenoTipoMisionDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DesempenoTipoMisionRowMapper implements RowMapper<DesempenoTipoMisionDTO> {

    @Override
    public DesempenoTipoMisionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DesempenoTipoMisionDTO(
                rs.getString("nombre_tipo"),
                rs.getString("nombre_modelo"),
                rs.getLong("total_completadas")
        );
    }
}