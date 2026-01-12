package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.model.Mision;
import com.Lab1BDA.Backend.repository.mappers.*;
// YA NO NECESITAMOS 'org.postgis.PGgeometry'
// IMPORTAMOS EL ESCRITOR DE WKB
import org.locationtech.jts.io.WKBWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types; // Importamos Types
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class MisionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String BASE_SELECT = "SELECT id_mision, id_dron_asignado, id_tipo_mision, " +
            "id_operador_creador, fecha_creacion, fecha_inicio_planificada, fecha_fin_planificada, " +
            "fecha_inicio_real, fecha_fin_real, estado, ST_AsText(ruta::geometry) AS ruta_wkt FROM misiones";

    public List<Mision> findAll() {
        return jdbcTemplate.query(BASE_SELECT, new MisionRowMapper());
    }

    public Optional<Mision> findById(Long id) {
        String sql = BASE_SELECT + " WHERE id_mision = ?";
        List<Mision> misiones = jdbcTemplate.query(sql, new MisionRowMapper(), id);
        return misiones.isEmpty() ? Optional.empty() : Optional.of(misiones.get(0));
    }

    public Mision save(Mision mision) {
        String sql = "INSERT INTO misiones (id_dron_asignado, id_tipo_mision, id_operador_creador, " +
                "fecha_inicio_planificada, fecha_fin_planificada, estado, ruta) " +
                "VALUES (?, ?, ?, ?, ?, CAST(? AS estado_mision), ST_GeogFromWKB(?))";

        KeyHolder keyHolder = new GeneratedKeyHolder();


        // Creamos un WKBWriter para convertir el objeto JTS a bytes
        WKBWriter wkbWriter = new WKBWriter();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setObject(1, mision.getIdDronAsignado());
            ps.setLong(2, mision.getIdTipoMision());
            ps.setLong(3, mision.getIdOperadorCreador());
            ps.setObject(4, mision.getFechaInicioPlanificada());
            ps.setObject(5, mision.getFechaFinPlanificada());
            ps.setString(6, mision.getEstado());

            // Convertimos el LineString de JTS a un array de bytes (WKB)
            if (mision.getRuta() != null) {
                ps.setBytes(7, wkbWriter.write(mision.getRuta()));
            } else {
                // El tipo SQL para 'bytea' (WKB) es BINARY
                ps.setNull(7, Types.BINARY);
            }
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id_mision")) {
            mision.setIdMision(((Number) keyHolder.getKeys().get("id_mision")).longValue());
        }
        return mision;
    }

    public Mision update(Mision mision) {
        String sql = "UPDATE misiones SET id_dron_asignado = ?, id_tipo_mision = ?, " +
                "fecha_inicio_planificada = ?, fecha_fin_planificada = ?, fecha_inicio_real = ?, " +
                "fecha_fin_real = ?, estado = CAST(? AS estado_mision), ruta = ST_GeogFromWKB(?) " +
                "WHERE id_mision = ?";

        // Convertimos el LineString a byte[] antes de pasarlo al update
        byte[] rutaWkb = (mision.getRuta() != null)
                ? new WKBWriter().write(mision.getRuta())
                : null;

        jdbcTemplate.update(sql,
                mision.getIdDronAsignado(),
                mision.getIdTipoMision(),
                mision.getFechaInicioPlanificada(),
                mision.getFechaFinPlanificada(),
                mision.getFechaInicioReal(),
                mision.getFechaFinReal(),
                mision.getEstado(),
                rutaWkb, // Pasamos el array de bytes
                mision.getIdMision()
        );
        return mision;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM misiones WHERE id_mision = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Llama al procedimiento almacenado 'asignar_mision_a_dron' en la base de datos.
     * @param idMision El ID de la misión a asignar.
     * @param idDron El ID del dron a ser asignado.
     */
    public void asignarMisionADron(Long idMision, Long idDron) {
        // Usamos JdbcTemplate para llamar a un procedimiento almacenado
        // que no devuelve un ResultSet (devuelve VOID)

        // El 'SELECT' es la forma de ejecutar una función que devuelve VOID en PostgreSQL
        String sql = "SELECT asignar_mision_a_dron(?, ?)";

        // Simplemente ejecutamos la llamada.
        // Si el procedimiento almacenado (la función) lanza una EXCEPTION
        // (por ej, dron no disponible), jdbcTemplate.update() la
        // relanzará como una DataAccessException de Spring,
        // que nuestro servicio y controlador manejarán como un error.
        jdbcTemplate.update(sql, idMision, idDron);
    }

    /**
     * Consulta para el Requisito #3: Comparación de Desempeño por Tipo de Misión.
     * @return Lista de DesempenoTipoMisionDTO
     */
    public List<DesempenoTipoMisionDTO> findDesempenoPorTipoMision() {
        // Usamos la corrección '::estado_mision' para el ENUM
        String sql = "WITH Top2Models AS ( " +
                "    SELECT " +
                "        d.id_modelo " +
                "    FROM misiones m " +
                "    JOIN drones d ON m.id_dron_asignado = d.id_dron " +
                "    WHERE d.id_modelo IS NOT NULL " +
                "    GROUP BY d.id_modelo " +
                "    ORDER BY COUNT(m.id_mision) DESC " +
                "    LIMIT 2 " +
                ") " +
                "SELECT " +
                "    tm.nombre_tipo, " +
                "    md.nombre_modelo, " +
                "    COUNT(m.id_mision) AS total_completadas " +
                "FROM misiones m " +
                "JOIN tipos_mision tm ON m.id_tipo_mision = tm.id_tipo_mision " +
                "JOIN drones d ON m.id_dron_asignado = d.id_dron " +
                "JOIN modelos_dron md ON d.id_modelo = md.id_modelo " +
                "WHERE " +
                "    m.estado = 'Completada'::estado_mision " +
                "    AND d.id_modelo IN (SELECT id_modelo FROM Top2Models) " +
                "GROUP BY tm.nombre_tipo, md.nombre_modelo " +
                "ORDER BY tm.nombre_tipo, md.nombre_modelo";

        return jdbcTemplate.query(sql, new DesempenoTipoMisionRowMapper());
    }

    /**
     * Consulta para el Requisito #4: Detección de Patrones de Consumo de Batería.
     * @return Lista de BateriaConsumoDTO
     */
    public List<BateriaConsumoDTO> findPatronConsumoBateria() {
        // Esta consulta SQL implementa la lógica del Requisito #4
        String sql = "WITH ConsumoMision AS ( " +
                "    SELECT " +
                "        id_mision, " +
                "        (FIRST_VALUE(nivel_bateria_porcentaje) OVER (PARTITION BY id_mision ORDER BY \"timestamp\" ASC)) " +
                "        - " +
                "        (LAST_VALUE(nivel_bateria_porcentaje) OVER (PARTITION BY id_mision ORDER BY \"timestamp\" ASC " +
                "                                                ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)) " +
                "        AS consumo_bateria " +
                "    FROM registro_vuelo " +
                "    GROUP BY id_mision, nivel_bateria_porcentaje, \"timestamp\" " +
                "), " +
                "ConsumoMisionAgregado AS ( " +
                "    SELECT id_mision, MAX(consumo_bateria) AS consumo_total " +
                "    FROM ConsumoMision " +
                "    WHERE consumo_bateria IS NOT NULL AND consumo_bateria > 0 " +
                "    GROUP BY id_mision " +
                "), " +
                "DuracionMision AS ( " +
                "    SELECT " +
                "        id_mision, " +
                "        EXTRACT(EPOCH FROM (fecha_fin_real - fecha_inicio_real)) / 60.0 AS duracion_minutos " +
                "    FROM misiones " +
                "    WHERE estado = 'Completada'::estado_mision AND fecha_fin_real IS NOT NULL AND fecha_inicio_real IS NOT NULL " +
                "), " +
                "MisionStats AS ( " +
                "    SELECT " +
                "        d.id_mision, d.duracion_minutos, c.consumo_total " +
                "    FROM DuracionMision d " +
                "    JOIN ConsumoMisionAgregado c ON d.id_mision = c.id_mision " +
                "    WHERE d.duracion_minutos IS NOT NULL AND c.consumo_total IS NOT NULL " +
                "), " +
                "MisionesConRanking AS ( " +
                "    SELECT " +
                "        id_mision, duracion_minutos, consumo_total, " +
                "        NTILE(5) OVER (ORDER BY duracion_minutos ASC) AS quintil_duracion " +
                "    FROM MisionStats " +
                "), " +
                "UmbralConsumo AS ( " +
                "    SELECT MIN(consumo_total) AS umbral_consumo " +
                "    FROM MisionesConRanking " +
                "    WHERE quintil_duracion = 1 " +
                ") " +
                "SELECT " +
                "    mr.id_mision, " +
                "    mr.duracion_minutos, " +
                "    mr.consumo_total AS consumo_bateria " +
                "FROM MisionesConRanking mr, UmbralConsumo uc " +
                "WHERE " +
                "    mr.consumo_total < uc.umbral_consumo " +
                "ORDER BY " +
                "    mr.duracion_minutos DESC " +
                "LIMIT 3";

        return jdbcTemplate.query(sql, new BateriaConsumoRowMapper());
    }

    /**
     * Consulta para el Requisito #5: Análisis de Desempeño Mensual (Funciones de Ventana).
     * @return Lista de DesempenoMensualDTO
     */
    public List<DesempenoMensualDTO> findDesempenoMensual() {
        // Esta consulta SQL implementa la lógica del Requisito #5
        String sql = "WITH MisionesCompletadas AS ( " +
                "    SELECT " +
                "        id_mision, " +
                "        fecha_fin_real " +
                "    FROM misiones " +
                "    WHERE " +
                "        estado = 'Completada'::estado_mision " +
                "        AND fecha_fin_real >= (CURRENT_DATE - INTERVAL '1 year') " +
                "), " +
                "MisionesPorSemana AS ( " +
                "    SELECT " +
                "        DATE_TRUNC('month', fecha_fin_real) AS mes, " +
                "        DATE_TRUNC('week', fecha_fin_real) AS semana, " +
                "        COUNT(id_mision) AS total_semanal " +
                "    FROM MisionesCompletadas " +
                "    GROUP BY mes, semana " +
                "), " +
                "PromedioMensual AS ( " +
                "    SELECT " +
                "        TO_CHAR(mes, 'YYYY-MM') AS mes_formato, " +
                "        AVG(total_semanal) AS promedio_semanal " +
                "    FROM MisionesPorSemana " +
                "    GROUP BY mes " +
                "    ORDER BY mes " +
                "), " +
                "AnalisisVentana AS ( " +
                "    SELECT " +
                "        mes_formato AS mes, " +
                "        promedio_semanal, " +
                "        LAG(promedio_semanal, 1, 0.0) OVER (ORDER BY mes_formato) AS promedio_mes_anterior " +
                "    FROM PromedioMensual " +
                ") " +
                "SELECT " +
                "    mes, " +
                "    promedio_semanal, " +
                "    (promedio_semanal - promedio_mes_anterior) AS diferencia_mes_anterior " +
                "FROM AnalisisVentana " +
                "ORDER BY mes DESC";

        return jdbcTemplate.query(sql, new DesempenoMensualRowMapper());
    }

    /**
     * Consulta para el Requisito #10: Resumen de Misiones por Tipo (Vista Materializada).
     * @return Lista de ResumenMisionTipoDTO
     */
    public List<ResumenMisionTipoDTO> findResumenMisionesCompletadas() {
        // La consulta es simple porque el trabajo pesado ya lo hizo la vista materializada.
        String sql = "SELECT nombre_tipo, cantidad_total, promedio_horas " +
                "FROM resumen_misiones_completadas " +
                "ORDER BY nombre_tipo";

        return jdbcTemplate.query(sql, new ResumenMisionTipoRowMapper());
    }

    /**
     * Calcula la matriz de distancias entre misiones usando PostGIS (ST_Distance).
     * @param idsMisiones Lista de ID de las misiones a calcular su distancia
     * @return Lista de distancia entre el origen y el destino de una misión
     */
    public List<DistanciaMisionDTO> calcularMatrizDistancias(List<Long> idsMisiones) {
        if (idsMisiones == null || idsMisiones.isEmpty()) {
            return Collections.emptyList();
        }

        // Generamos los signos de interrogación: "?, ?, ?"
        String placeholders = String.join(",", Collections.nCopies(idsMisiones.size(), "?"));

        // Construimos el SQL con el mismo estilo que findDesempenoMensual
        // Usamos String.format para inyectar los placeholders en los dos lugares donde van (%s)
        //language=SQL
        String sqlPattern = "SELECT " +
                "    a.id_mision AS id_origen, " +
                "    b.id_mision AS id_destino, " +
                "    ST_Distance( " +
                "        ST_Centroid(a.ruta::geometry)::geography, " +
                "        ST_Centroid(b.ruta::geometry)::geography " +
                "    ) AS distancia_metros " +
                "FROM misiones a " +
                "CROSS JOIN misiones b " +
                "WHERE " +
                "    a.id_mision IN (%s) " +
                "    AND b.id_mision IN (%s) " +
                "    AND a.id_mision != b.id_mision";

        // Inyectamos los placeholders
        String sql = String.format(sqlPattern, placeholders, placeholders);

        // Preparamos los argumentos (la lista duplicada porque aparece 2 veces en la SQL)
        Object[] args = new Object[idsMisiones.size() * 2];
        for (int i = 0; i < idsMisiones.size(); i++) {
            args[i] = idsMisiones.get(i);
            args[idsMisiones.size() + i] = idsMisiones.get(i);
        }

        return jdbcTemplate.query(sql, new DistanciaMisionRowMapper(), args);
    }

    /**
     * Recupera los detalles completos (fechas, ruta, tipo) de las misiones seleccionadas.
     * Es vital para calcular la duración y pintar el mapa al final.
     * @param ids Lista de los id de las misiones
     * @return Lista de misiones con todos los datos
     */
    public List<Mision> findMisionesPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // Generamos los placeholders: "?, ?, ?"
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));

        // Reutilizamos la BASE_SELECT ya definida al inicio del archivo para no repetir todas las columnas.
        String sql = BASE_SELECT + " WHERE id_mision IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, new MisionRowMapper(), ids.toArray());
    }

}