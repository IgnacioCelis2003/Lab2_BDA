package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.model.RegistroVuelo;
import com.Lab1BDA.Backend.repository.mappers.RegistroVueloRowMapper;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RegistroVueloRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Consulta base para LEER registros (usando ST_AsWKT como en MisionRowMapper)
    private final String BASE_SELECT = "SELECT id_registro_vuelo, id_mision, \"timestamp\", " +
            "altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje, " +
            "ST_AsText(coordenadas) AS coordenadas_wkt " +
            "FROM registro_vuelo";

    // WKBWriter para convertir geometrías al guardar. '3' indica 3 dimensiones (X, Y, Z). 'true' incluye el SRID.
    private final WKBWriter wkbWriter = new WKBWriter(3, true);

    /**
     * Guarda un nuevo registro de telemetría en la base de datos.
     * @param registro El objeto RegistroVuelo a guardar.
     * @return El objeto RegistroVuelo guardado (con su ID).
     */
    public RegistroVuelo save(RegistroVuelo registro) {
        // La consulta SQL usa ST_GeogFromWKB para interpretar los bytes que enviamos
        String sql = "INSERT INTO registro_vuelo (id_mision, \"timestamp\", coordenadas, " +
                "altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje) " +
                "VALUES (?, ?, ST_GeogFromWKB(?), ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, registro.getIdMision());
            ps.setObject(2, registro.getTimestamp());

            // Convertimos el objeto Point (que ya debe tener Z) a bytes
            if (registro.getCoordenadas() != null) {
                ps.setBytes(3, wkbWriter.write(registro.getCoordenadas()));
            } else {
                ps.setNull(3, Types.BINARY);
            }

            ps.setObject(4, registro.getAltitudMsnm());
            ps.setObject(5, registro.getVelocidadKmh());
            ps.setDouble(6, registro.getNivelBateriaPorcentaje());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id_registro_vuelo")) {
            registro.setIdRegistroVuelo(((Number) keyHolder.getKeys().get("id_registro_vuelo")).longValue());
        }
        return registro;
    }

    /**
     * Busca todos los registros de telemetría para una misión específica.
     * @param idMision El ID de la misión.
     * @return Una lista de registros de vuelo, ordenados por timestamp.
     */
    public List<RegistroVuelo> findByMisionId(Long idMision) {
        String sql = BASE_SELECT + " WHERE id_mision = ? ORDER BY \"timestamp\" ASC";
        return jdbcTemplate.query(sql, new RegistroVueloRowMapper(), idMision);
    }

    /**
     * Busca todos los registros de telemetría con cierto timestamp.
     * @param timestamp
     * @return
     */
    public List<RegistroVuelo> findByTimestampInterval(LocalDateTime timestamp) {
        String sql = BASE_SELECT + " WHERE timestamp BETWEEN ? AND ?";
        LocalDateTime finIntervalo = timestamp.plusSeconds(5);
        return jdbcTemplate.query(sql, new RegistroVueloRowMapper(), timestamp, finIntervalo);
    }

    /**
     * Obtiene el registro más reciente de cada misión que está en estado 'En Progreso'.
     * @return Lista con un registro por cada misión activa.
     */
    public List<RegistroVuelo> findLatestByMisionWithActiveStatus() {
        String sql = "SELECT DISTINCT ON (rv.id_mision) " +
                "rv.id_registro_vuelo, rv.id_mision, rv.\"timestamp\", " +
                "rv.altitud_msnm, rv.velocidad_kmh, rv.nivel_bateria_porcentaje, " +
                "ST_AsText(rv.coordenadas) AS coordenadas_wkt " +
                "FROM registro_vuelo rv " +
                "INNER JOIN misiones m ON rv.id_mision = m.id_mision " +
                "WHERE m.estado = 'En Progreso' " +
                "ORDER BY rv.id_mision, rv.\"timestamp\" DESC";
        return jdbcTemplate.query(sql, new RegistroVueloRowMapper());
    }
    public Double calcularLongitudTrayectoria(Long misionId) {
        // Usamos el campo de tiempo para asegurar la cronología real
        // Reemplaza 'fecha' por el nombre exacto de tu columna de tiempo
        String sql = """
        SELECT ST_Length(
            ST_MakeLine(coordenadas ORDER BY timestamp ASC)::geography
        )
        FROM registro_vuelo
        WHERE id_mision = ?
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Double.class, misionId);
        } catch (Exception e) {
            return 0.0;
        }
    }

}