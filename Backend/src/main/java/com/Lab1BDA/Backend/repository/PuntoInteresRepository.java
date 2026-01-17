package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.model.PuntoInteres;
import com.Lab1BDA.Backend.repository.mappers.PuntoInteresRowMapper;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PuntoInteresRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // '3' indica que soportamos X, Y, Z (Lat, Lon, Alt). 'true' incluye el SRID (4326).
    private final WKBWriter wkbWriter = new WKBWriter(3, true);


    /**
     * Obtiene todos los puntos de interés.
     */
    public List<PuntoInteres> findAll() {
        String sql= "SELECT poi_id, nombre, descripcion, ST_AsText(ubicacion) as ubicacion_wkt FROM puntos_interes";
        return jdbcTemplate.query(sql, new PuntoInteresRowMapper());
    }

    /**
     * Busca un POI por su ID.
     */
    public Optional<PuntoInteres> findById(Long id) {
        String sql = "SELECT poi_id, nombre, descripcion, ST_AsText(ubicacion) as ubicacion_wkt FROM puntos_interes" + " WHERE poi_id = ?";
        List<PuntoInteres> results = jdbcTemplate.query(sql, new PuntoInteresRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Guarda un nuevo Punto de Interés.
     */
    public PuntoInteres save(PuntoInteres poi) {
        String sql = "INSERT INTO puntos_interes (nombre, descripcion, ubicacion) VALUES (?, ?, ST_GeomFromWKB(?, 4326))";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, poi.getNombre());
            ps.setString(2, poi.getDescripcion());

            // Convertimos el objeto Point de JTS a binario (WKB) para PostGIS
            byte[] wkb = (poi.getUbicacion() != null) ? wkbWriter.write(poi.getUbicacion()) : null;
            ps.setBytes(3, wkb);

            return ps;
        }, keyHolder);

        // Recuperar el ID generado
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("poi_id")) {
            poi.setPoi_id(((Number) keyHolder.getKeys().get("poi_id")).longValue());
        }

        return poi;
    }

    /**
     * Actualiza un POI existente.
     */
    public void update(PuntoInteres poi) {
        String sql = "UPDATE puntos_interes SET nombre = ?, descripcion = ?, ubicacion = ST_GeomFromWKB(?, 4326) WHERE poi_id = ?";

        byte[] wkb = (poi.getUbicacion() != null) ? wkbWriter.write(poi.getUbicacion()) : null;

        jdbcTemplate.update(sql,
                poi.getNombre(),
                poi.getDescripcion(),
                wkb,
                poi.getPoi_id()
        );
    }

    /**
     * Elimina un POI.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM puntos_interes WHERE poi_id = ?";
        jdbcTemplate.update(sql, id);
    }


    /**
     * Calcula la distancia 3D mínima en METROS entre la trayectoria de una misión
     * y un punto de interés.
     */
    public Double calcularDistanciaMinima3D(Long misionId, Long poiId) {
        // Usamos ::geography para forzar el cálculo en metros.
        String sql = """
        SELECT MIN(ST_3DDistance(rv.coordenadas::geography, poi.ubicacion::geography))
        FROM registro_vuelo rv
        JOIN puntos_interes poi ON poi.poi_id = ?
        WHERE rv.id_mision = ?
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Double.class, poiId, misionId);
        } catch (Exception e) {
            // Si no hay registros de vuelo, devolvemos null o 0.0
            return null;
        }
    }



}