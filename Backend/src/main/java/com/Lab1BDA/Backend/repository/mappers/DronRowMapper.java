package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.Dron;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DronRowMapper implements RowMapper<Dron> {

    @Override
    public Dron mapRow(ResultSet rs, int rowNum) throws SQLException {
        Dron dron = new Dron();
        dron.setIdDron(rs.getLong("id_dron"));
        dron.setIdModelo(rs.getLong("id_modelo"));
        dron.setEstado(rs.getString("estado"));
        return dron;
    }
}