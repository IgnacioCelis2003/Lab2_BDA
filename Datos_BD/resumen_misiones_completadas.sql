-- Requisito 10: Creación de la Vista Materializada

-- 1. Creamos la vista materializada
CREATE MATERIALIZED VIEW resumen_misiones_completadas AS
SELECT
    -- Seleccionamos el nombre del tipo de misión
    tm.nombre_tipo,
    -- Contamos la cantidad total de misiones
    COUNT(m.id_mision) AS cantidad_total,
    -- Calculamos el tiempo promedio de vuelo EN HORAS
    AVG(EXTRACT(EPOCH FROM (m.fecha_fin_real - m.fecha_inicio_real)) / 3600.0) AS promedio_horas
FROM
    misiones m
-- Unimos con tipos_mision para obtener el nombre
        JOIN tipos_mision tm ON m.id_tipo_mision = tm.id_tipo_mision
WHERE
    -- Filtramos solo por misiones 'Completada'
    m.estado = 'Completada'::estado_mision
-- Agrupamos por el nombre del tipo de misión
GROUP BY
    tm.nombre_tipo;


-- 2. Creamos un ÍNDICE ÚNICO (¡Requerido para refrescar concurrentemente!)
-- El enunciado pide "refrescada concurrenemente", y para eso,
-- la vista DEBE tener un índice único.
CREATE UNIQUE INDEX idx_resumen_misiones_tipo_unico
    ON resumen_misiones_completadas (nombre_tipo);