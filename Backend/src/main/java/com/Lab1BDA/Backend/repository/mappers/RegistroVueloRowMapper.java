package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.RegistroVuelo;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class RegistroVueloRowMapper implements RowMapper<RegistroVuelo> {

    // Usamos un WKTReader para convertir el String de la BD a un objeto Point
    private final WKTReader wktReader = new WKTReader();

    @Override
    public RegistroVuelo mapRow(ResultSet rs, int rowNum) throws SQLException {
        RegistroVuelo registro = new RegistroVuelo();
        registro.setIdRegistroVuelo(rs.getLong("id_registro_vuelo"));
        registro.setIdMision(rs.getLong("id_mision"));
        registro.setTimestamp(rs.getObject("timestamp", LocalDateTime.class));

        // Manejo seguro de valores Double que pueden ser NULL
        Object altitudObj = rs.getObject("altitud_msnm");
        registro.setAltitudMsnm(altitudObj != null ? ((Number) altitudObj).doubleValue() : null);

        Object velocidadObj = rs.getObject("velocidad_kmh");
        registro.setVelocidadKmh(velocidadObj != null ? ((Number) velocidadObj).doubleValue() : null);

        registro.setNivelBateriaPorcentaje(rs.getDouble("nivel_bateria_porcentaje"));

        // --- Mapeo de GEOGRAPHY (PostGIS) a Point (JTS) ---
        // Leemos la columna 'coordenadas_wkt' que pedimos en el SQL
        String coordenadasWkt = rs.getString("coordenadas_wkt");

        if (coordenadasWkt != null && !coordenadasWkt.isEmpty()) {
            try {
                Geometry geom = wktReader.read(coordenadasWkt);
                if (geom instanceof Point) {
                    registro.setCoordenadas((Point) geom);
                }
            } catch (ParseException e) {
                throw new SQLException("Error al parsear WKT de coordenadas: " + coordenadasWkt, e);
            }
        }

        return registro;
    }
}