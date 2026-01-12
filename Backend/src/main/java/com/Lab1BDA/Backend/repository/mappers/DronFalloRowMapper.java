package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.dto.DronFalloDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DronFalloRowMapper implements RowMapper<DronFalloDTO> {

    @Override
    public DronFalloDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DronFalloDTO(
                rs.getLong("id_dron"),
                rs.getLong("total_misiones_fallidas")
        );
    }
}