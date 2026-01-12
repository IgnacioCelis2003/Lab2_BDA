package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.Mision;
// Importamos las clases de JTS
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
// Ya no necesitamos org.postgis.PGgeometry aquí
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class MisionRowMapper implements RowMapper<Mision> {

    // Creamos un WKTReader para parsear el String que viene de la BD
    private final WKTReader wktReader = new WKTReader();

    @Override
    public Mision mapRow(ResultSet rs, int rowNum) throws SQLException {
        Mision mision = new Mision();
        mision.setIdMision(rs.getLong("id_mision"));
        Object idDron = rs.getObject("id_dron_asignado");
        if (idDron != null) {
            mision.setIdDronAsignado(((Number) idDron).longValue());
        }
        mision.setIdTipoMision(rs.getLong("id_tipo_mision"));
        mision.setIdOperadorCreador(rs.getLong("id_operador_creador"));

        mision.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        mision.setFechaInicioPlanificada(rs.getObject("fecha_inicio_planificada", LocalDateTime.class));
        mision.setFechaFinPlanificada(rs.getObject("fecha_fin_planificada", LocalDateTime.class));
        mision.setFechaInicioReal(rs.getObject("fecha_inicio_real", LocalDateTime.class));
        mision.setFechaFinReal(rs.getObject("fecha_fin_real", LocalDateTime.class));

        mision.setEstado(rs.getString("estado"));

        // --- Mapeo Corregido de GEOGRAPHY ---
        // 1. Leemos el String WKT de la columna 'ruta_wkt'
        String rutaWkt = rs.getString("ruta_wkt");

        if (rutaWkt != null && !rutaWkt.isEmpty()) {
            try {
                // 2. Usamos el WKTReader para convertir el String a un objeto Geometry de JTS
                Geometry geom = wktReader.read(rutaWkt);

                // 3. Verificamos que sea un LineString (como esperamos) y lo asignamos
                if (geom instanceof LineString) {
                    mision.setRuta((LineString) geom);
                }
            } catch (ParseException e) {
                // Si el WKT está malformado, lanzamos una excepción
                throw new SQLException("Error al parsear WKT de la ruta para la misión id: " + mision.getIdMision(), e);
            }
        }

        return mision;
    }
}