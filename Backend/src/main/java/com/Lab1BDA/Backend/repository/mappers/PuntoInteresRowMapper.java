package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.PuntoInteres;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PuntoInteresRowMapper implements RowMapper<PuntoInteres> {

    // El WKTReader nos permite convertir el String de PostGIS a un objeto Point de JTS
    private final WKTReader wktReader = new WKTReader();

    @Override
    public PuntoInteres mapRow(ResultSet rs, int rowNum) throws SQLException {
        PuntoInteres poi = new PuntoInteres();
        poi.setPoi_id(rs.getLong("poi_id"));
        poi.setNombre(rs.getString("nombre"));
        poi.setDescripcion(rs.getString("descripcion"));

        // Extraemos la geometría que viene como WKT desde la consulta SQL
        String wkt = rs.getString("ubicacion_wkt");
        if (wkt != null) {
            try {
                poi.setUbicacion((Point) wktReader.read(wkt));
            } catch (ParseException e) {
                // Manejo de error si el formato WKT es inválido
                throw new SQLException("Error al parsear la ubicación del POI: " + wkt, e);
            }
        }

        return poi;
    }
}