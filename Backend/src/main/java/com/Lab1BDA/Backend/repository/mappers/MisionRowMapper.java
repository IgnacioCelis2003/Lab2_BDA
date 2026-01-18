package com.Lab1BDA.Backend.repository.mappers;

import com.Lab1BDA.Backend.model.Mision;
// Importamos las clases de JTS
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
// Ya no necesitamos org.postgis.PGgeometry aquí
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class MisionRowMapper implements RowMapper<Mision> {

    // Creamos un WKTReader para parsear el String que viene de la BD
    private final WKBReader wkbReader = new WKBReader();

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
        byte[] rutaBytes = rs.getBytes("ruta_bytes");

        if (rutaBytes != null && rutaBytes.length > 0) {
            try {
                Geometry geom = wkbReader.read(rutaBytes);
                if (geom instanceof LineString) {
                    mision.setRuta((LineString) geom);
                    // Opcional: Asegurar SRID 4326 si JTS no lo infiere
                    if (mision.getRuta().getSRID() == 0) {
                        mision.getRuta().setSRID(4326);
                    }
                }
            } catch (ParseException e) {
                throw new SQLException("Error al parsear geometría WKB", e);
            }
        }

        return mision;
    }
}