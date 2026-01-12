package com.Lab1BDA.Backend.repository;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.model.Dron;
import com.Lab1BDA.Backend.repository.mappers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class DronRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Guarda un nuevo dron en la base de datos.
     * @param dron El objeto Dron a guardar (sin ID)
     * @return El objeto Dron guardado (CON el ID generado por la BD)
     */
    public Dron save(Dron dron) {
        String sql = "INSERT INTO drones (id_modelo, estado) VALUES (?, CAST(? AS estado_dron))";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dron.getIdModelo());
            ps.setString(2, dron.getEstado());
            return ps;
        }, keyHolder);

        // Obtenemos el ID generado y lo asignamos al objeto dron
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id_dron")) {
            dron.setIdDron(((Number) keyHolder.getKeys().get("id_dron")).longValue());
        }

        return dron;
    }

    /**
     * Actualiza un dron existente en la base de datos.
     * @param dron El objeto Dron con los datos actualizados.
     * @return El objeto Dron actualizado.
     */
    public Dron update(Dron dron) {
        String sql = "UPDATE drones SET id_modelo = ?, estado = CAST(? AS estado_dron) WHERE id_dron = ?";
        jdbcTemplate.update(sql, dron.getIdModelo(), dron.getEstado(), dron.getIdDron());
        return dron;
    }

    /**
     * Elimina un dron de la base de datos por su ID.
     * @param id El ID del dron a eliminar.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM drones WHERE id_dron = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Obtiene todos los drones de la base de datos.
     * @return una lista de objetos Dron.
     */
    public List<Dron> findAll() {
        String sql = "SELECT id_dron, id_modelo, estado FROM drones";
        return jdbcTemplate.query(sql, new DronRowMapper());
    }

    /**
     * Busca un dron por su ID.
     * @param id El ID del dron a buscar.
     * @return Un Optional<Dron>
     */
    public Optional<Dron> findById(Long id) {
        String sql = "SELECT id_dron, id_modelo, estado FROM drones WHERE id_dron = ?";
        List<Dron> drones = jdbcTemplate.query(sql, new DronRowMapper(), id);
        return drones.isEmpty() ? Optional.empty() : Optional.of(drones.get(0));
    }

    /**
     * Llama al procedimiento almacenado 'actualizar_mantenimiento_por_modelo' en la BD.
     * @param idModelo El ID del modelo de dron.
     * @return El número de drones que fueron actualizados a 'En Mantenimiento'.
     */
    public int llamarActualizacionMantenimiento(Long idModelo) {
        String sql = "SELECT actualizar_mantenimiento_por_modelo(?)";

        // Usamos queryForObject porque la función de PostgreSQL devuelve un solo valor (el conteo)
        Integer conteo = jdbcTemplate.queryForObject(sql, Integer.class, idModelo);

        return (conteo != null) ? conteo : 0;
    }

    /**
     * Consulta para el Requisito #8: Listado de Drones Inactivos.
     * Muestra drones que no han volado en los últimos 30 días o que nunca han volado.
     * @return Lista de DronInactivoDTO
     */
    public List<DronInactivoDTO> findDronesInactivos() {
        // Esta consulta SQL implementa la lógica del Requisito #8
        String sql = "SELECT " +
                "    d.id_dron, " +
                "    md.nombre_modelo, " +
                "    d.estado, " +
                // 1. Obtenemos la fecha máxima (última misión)
                "    MAX(m.fecha_fin_real)::date AS ultima_mision " +
                "FROM drones d " +
                // 2. Unimos con modelos_dron para obtener el nombre
                "LEFT JOIN modelos_dron md ON d.id_modelo = md.id_modelo " +
                // 3. Unimos con misiones para ver sus vuelos
                "LEFT JOIN misiones m ON d.id_dron = m.id_dron_asignado " +
                "GROUP BY d.id_dron, md.nombre_modelo, d.estado " +
                // 4. Filtramos con HAVING
                "HAVING " +
                //    (La última misión fue hace más de 30 días)
                "    MAX(m.fecha_fin_real) < (CURRENT_DATE - INTERVAL '30 days') " +
                //    (O el dron nunca ha tenido una misión)
                "    OR MAX(m.fecha_fin_real) IS NULL";

        return jdbcTemplate.query(sql, new DronInactivoRowMapper());
    }

    /**
     * Consulta para el Requisito #1: Análisis de Duración de Vuelo.
     * Calcula el tiempo total de vuelo (en horas) por modelo de dron
     * para misiones completadas en el último mes.
     * @return Lista de DuracionVueloDTO
     */
    public List<DuracionVueloDTO> findDuracionVueloPorModeloUltimoMes() {
        // Esta consulta SQL implementa la lógica del Requisito #1
        String sql = "SELECT " +
                "    md.nombre_modelo, " +
                // 1. Sumamos la duración en segundos y la dividimos por 3600 para obtener horas
                "    SUM(EXTRACT(EPOCH FROM (m.fecha_fin_real - m.fecha_inicio_real))) / 3600.0 AS tiempo_total_horas " +
                "FROM misiones m " +
                // 2. Unimos con drones para obtener el id_modelo
                "JOIN drones d ON m.id_dron_asignado = d.id_dron " +
                // 3. Unimos con modelos_dron para obtener el nombre del modelo
                "JOIN modelos_dron md ON d.id_modelo = md.id_modelo " +
                "WHERE " +
                // 4. Filtramos por misiones 'Completada'
                "    m.estado = 'Completada' " +
                // 5. Y que hayan terminado en el último mes
                "    AND m.fecha_fin_real >= (CURRENT_DATE - INTERVAL '1 month') " +
                // 6. Agrupamos por el nombre del modelo
                "GROUP BY md.nombre_modelo " +
                // 7. Ordenamos de mayor a menor tiempo
                "ORDER BY tiempo_total_horas DESC";

        return jdbcTemplate.query(sql, new DuracionVueloRowMapper());
    }

    /**
     * Consulta para el Requisito #2: Drones con Fallos Recurrentes.
     * Identifica los 5 drones con más misiones en estado 'Fallida'.
     * @return Lista de DronFalloDTO
     */
    public List<DronFalloDTO> findDronesConMasFallos() {
        // Esta consulta SQL implementa la lógica del Requisito #2 usando una CTE
        String sql = "WITH MisionesFallidas AS ( " +
                "    SELECT " +
                "        id_dron_asignado, " +
                "        COUNT(*) AS total_misiones_fallidas " +
                "    FROM misiones " +
                "    WHERE " +
                "        estado = 'Fallida' " +
                "        AND id_dron_asignado IS NOT NULL " +
                "    GROUP BY id_dron_asignado " +
                ") " +

                 "SELECT " +
                "    id_dron_asignado AS id_dron, " +
                "    total_misiones_fallidas " +
                "FROM MisionesFallidas " +
                "ORDER BY total_misiones_fallidas DESC " +
                "LIMIT 5";

        return jdbcTemplate.query(sql, new DronFalloRowMapper());
    }

    /**
     * Consulta para el Requisito #9: Análisis Geográfico de Puntos de Interés.
     * @param longitud La longitud del punto de interés.
     * @param latitud La latitud del punto de interés.
     * @return Lista de AnalisisGeograficoDTO
     */
    public List<AnalisisGeograficoDTO> findDronesCercanosPunto(double longitud, double latitud) {
        // Esta consulta SQL implementa la lógica del Requisito #9
        // Usamos ST_Point(lon, lat)::geography para crear el punto de interés
        // Usamos ST_Distance para calcular la distancia en metros (gracias a geography)
        String sql = "SELECT " +
                "    m.id_dron_asignado AS id_dron, " +
                "    MIN(ST_Distance(rv.coordenadas, ST_Point(?, ?)::geography)) AS distancia_minima_metros " +
                "FROM registro_vuelo rv " +
                // 1. Unimos con misiones para saber qué dron hizo el vuelo
                "JOIN misiones m ON rv.id_mision = m.id_mision " +
                "WHERE " +
                // 2. Filtramos por el último mes
                "    rv.\"timestamp\" >= (CURRENT_DATE - INTERVAL '1 month') " +
                "    AND m.id_dron_asignado IS NOT NULL " +
                // 3. Agrupamos por dron
                "GROUP BY m.id_dron_asignado " +
                // 4. Ordenamos por la distancia mínima
                "ORDER BY distancia_minima_metros ASC " +
                // 5. Limitamos a los 5 más cercanos
                "LIMIT 5";

        return jdbcTemplate.query(sql, new AnalisisGeograficoRowMapper(), longitud, latitud);
    }

    /**
     * Consulta para funcionalidad clave del sistema: optimización de rutas
     * Obtiene especificaciones técnicas (autonomía, carga) realizando JOIN con modelos_dron.
     * Filtra para traer solo drones con estado 'Disponible'.
     * @return Lista de DronSpecsDTO
     */
    public List<DronSpecsDTO> findDronesDisponiblesConSpecs() {
        String sql = "SELECT " +
                "    d.id_dron, " +
                "    md.nombre_modelo, " +
                "    md.autonomia_minutos, " +
                "    md.capacidad_carga_kg, " +
                "    md.velocidad_promedio_kmh " +
                "FROM drones d " +
                // 1. Unimos con modelos_dron para obtener autonomía y carga
                "JOIN modelos_dron md ON d.id_modelo = md.id_modelo " +
                "WHERE " +
                // 2. Filtramos solo los que están "Disponible"
                "    d.estado = 'Disponible' ";

        return jdbcTemplate.query(sql, new DronSpecsRowMapper());
    }

    /**
     * Genera un reporte de misiones por cada dron (misiones completadas, fallidas y total ed hroas de vuelo)
     * @return lista de dron, modelo, total de misiones completadas y fallidas y horas totales de vuelo
     */
    public List<ReporteDesempenoDronDTO> findReporteDesempenoGlobal() {
        String sql = """
        SELECT 
            d.id_dron,
            md.nombre_modelo,
            -- Contamos condicionalmente según el estado
            COUNT(CASE WHEN m.estado = 'Completada' THEN 1 END) AS total_completadas,
            COUNT(CASE WHEN m.estado = 'Fallida' THEN 1 END) AS total_fallidas,
            -- Sumamos la diferencia de tiempo y la pasamos a horas (o 0 si es nulo)
            COALESCE(
                SUM(EXTRACT(EPOCH FROM (m.fecha_fin_real - m.fecha_inicio_real))) / 3600.0, 
                0
            ) AS horas_vuelo
        FROM drones d
        JOIN modelos_dron md ON d.id_modelo = md.id_modelo
        LEFT JOIN misiones m ON d.id_dron = m.id_dron_asignado
        GROUP BY d.id_dron, md.nombre_modelo
        ORDER BY horas_vuelo DESC
        """;

        return jdbcTemplate.query(sql, new ReporteDesempenoRowMapper());
    }
}