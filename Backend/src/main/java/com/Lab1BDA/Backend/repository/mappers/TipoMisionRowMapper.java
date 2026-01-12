package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.TipoMision;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TipoMisionRowMapper implements RowMapper<TipoMision> {
    @Override
    public TipoMision mapRow(ResultSet rs, int rowNum) throws SQLException {
        TipoMision tipoMision = new TipoMision();
        tipoMision.setIdTipoMision(rs.getLong("id_tipo_mision"));
        tipoMision.setNombreTipo(rs.getString("nombre_tipo"));
        return tipoMision;
    }
}

