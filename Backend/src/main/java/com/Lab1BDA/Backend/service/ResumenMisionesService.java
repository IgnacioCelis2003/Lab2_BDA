package com.Lab1BDA.Backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio para manejar la actualización de la vista materializada
 * resumen_misiones_completadas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumenMisionesService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Refresca la vista materializada resumen_misiones_completadas.
     * Utiliza REFRESH MATERIALIZED VIEW CONCURRENTLY para no bloquear
     * las consultas existentes.
     */
    public void refreshResumenMisiones() {
        try {
            jdbcTemplate.execute(
                "REFRESH MATERIALIZED VIEW CONCURRENTLY resumen_misiones_completadas"
            );
            log.info("Vista materializada resumen_misiones_completadas actualizada exitosamente");
        } catch (Exception e) {
            log.error("Error al actualizar la vista materializada: ", e);
            throw new RuntimeException("Error al refrescar la vista materializada", e);
        }
    }
}

