-- Creación del Procedimiento Almacenado (Función en PostgreSQL)
-- Requisito 7 del enunciado: Actualización Masiva de Estado de Mantenimiento

CREATE OR REPLACE FUNCTION actualizar_mantenimiento_por_modelo(
    p_id_modelo BIGINT
)
RETURNS INTEGER AS $$ -- Devuelve el número de drones actualizados
DECLARE
v_drones_actualizados INTEGER;
BEGIN
WITH drones_con_tiempo AS (
    -- 1. Calcular el tiempo total de vuelo (en horas) para cada dron
    SELECT
        m.id_dron_asignado,
        -- Sumamos la diferencia en segundos, luego dividimos para obtener horas
        SUM(EXTRACT(EPOCH FROM (m.fecha_fin_real - m.fecha_inicio_real))) / 3600.0 AS horas_vuelo_total
    FROM misiones m
    WHERE m.id_dron_asignado IS NOT NULL
      AND m.estado = 'Completada'
      AND m.fecha_fin_real IS NOT NULL
      AND m.fecha_inicio_real IS NOT NULL
    GROUP BY m.id_dron_asignado
),
     drones_para_actualizar AS (
         -- 2. Identificar los drones del modelo dado que superan el umbral
         SELECT
             d.id_dron
         FROM drones d
                  JOIN drones_con_tiempo dt ON d.id_dron = dt.id_dron_asignado
         WHERE d.id_modelo = p_id_modelo
           AND dt.horas_vuelo_total > 100.0
           AND d.estado != 'En Mantenimiento' -- Evitar actualizar los que ya están
    )
-- 3. Actualizar el estado de los drones identificados
UPDATE drones
SET estado = 'En Mantenimiento'
WHERE id_dron IN (SELECT id_dron FROM drones_para_actualizar);

-- 4. Obtener el conteo de filas afectadas
GET DIAGNOSTICS v_drones_actualizados = ROW_COUNT;
RETURN v_drones_actualizados;

END;
$$ LANGUAGE plpgsql;