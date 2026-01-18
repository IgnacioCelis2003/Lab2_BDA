package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.dto.ZonaProhibidaDTO;
import com.Lab1BDA.Backend.model.ZonaProhibida;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ZonaProhibidaRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final WKTReader wktReader = new WKTReader();
    // Las zonas suelen ser áreas 2D en el suelo (SRID 4326)
    private final WKBWriter wkbWriter = new WKBWriter(2, true);

    public ZonaProhibida save(ZonaProhibida zona) {
        String sql = "INSERT INTO zonas_prohibidas (nombre, area) VALUES (?, ST_GeomFromWKB(?, 4326))";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, zona.getNombre());
            ps.setBytes(2, wkbWriter.write(zona.getArea()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("zona_id")) {
            zona.setId(((Number) keyHolder.getKeys().get("zona_id")).longValue());
        }
        return zona;
    }

    public List<ZonaProhibida> findAll() {
        String sql = "SELECT zona_id, nombre, ST_AsText(area) AS area_wkt FROM zonas_prohibidas";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long id = rs.getLong("zona_id");
            String nombre = rs.getString("nombre");
            String areaWkt = rs.getString("area_wkt");
            Polygon area = null;

            try {
                area = (Polygon) wktReader.read(areaWkt);
            } catch (Exception e) {
                throw new RuntimeException("Error al convertir WKT a Polygon para la zona: " + nombre, e);
            }

            return new ZonaProhibida(id, nombre, area);
        });
    }

    public List<ZonaProhibidaDTO> findAllAsString() {
        String sql = "SELECT zona_id, nombre, ST_AsText(area) AS area FROM zonas_prohibidas";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ZonaProhibidaDTO dto = new ZonaProhibidaDTO(
                    rs.getLong("zona_id"),
                    rs.getString("nombre"),
                    rs.getString("area"));
            return dto;
        });
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM zonas_prohibidas WHERE zona_id = ?";
        try {
            int rows = jdbcTemplate.update(sql, id);
            if (rows == 0) {
                throw new RuntimeException("No se encontró la zona prohibida con id: " + id);
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("Error al eliminar la zona prohibida con id: " + id, e);
        }
    }

    /**
     * Verifica si una geometría (Ruta o Punto) intersecta alguna zona prohibida.
     * @param geometriaWkt Geometría en formato WKT (Well-Known Text)
     * @return Lista de nombres de las zonas que se están infringiendo.
     */
    public List<String> encontrarInfracciones(String geometriaWkt) {
        // ST_Intersects funciona con GEOGRAPHY para detectar cruces reales
        String sql = "SELECT nombre FROM zonas_prohibidas " +
                "WHERE ST_Intersects(area, ST_GeogFromText(?)::geometry)";

        return jdbcTemplate.queryForList(sql, String.class, geometriaWkt);
    }
}