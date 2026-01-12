-- Creación del Procedimiento Almacenado (Función en PostgreSQL)
-- Requisito 6 del enunciado: Simulación de Asignación de Misiones

CREATE OR REPLACE FUNCTION asignar_mision_a_dron(
    p_id_mision BIGINT,
    p_id_dron BIGINT
)
RETURNS VOID AS $$
DECLARE
v_estado_dron estado_dron;
    v_estado_mision estado_mision;
BEGIN
    -- 1. Obtener y validar el estado del dron
    -- Se usa FOR UPDATE para bloquear la fila del dron y evitar concurrencia
SELECT estado INTO v_estado_dron
FROM drones
WHERE id_dron = p_id_dron
    FOR UPDATE;

IF v_estado_dron IS NULL THEN
        RAISE EXCEPTION 'El dron con ID % no existe.', p_id_dron;
END IF;

    IF v_estado_dron != 'Disponible' THEN
        RAISE EXCEPTION 'El dron % no está disponible. Estado actual: %', p_id_dron, v_estado_dron;
END IF;

    -- 2. Obtener y validar el estado de la misión
SELECT estado INTO v_estado_mision
FROM misiones
WHERE id_mision = p_id_mision
    FOR UPDATE;

IF v_estado_mision IS NULL THEN
        RAISE EXCEPTION 'La misión con ID % no existe.', p_id_mision;
END IF;

    IF v_estado_mision != 'Pendiente' THEN
        RAISE EXCEPTION 'La misión % no está pendiente. Estado actual: %', p_id_mision, v_estado_mision;
END IF;

    -- 3. Si todo es válido, realizar las actualizaciones
    -- (Todo esto ocurre en una transacción)

    -- Actualizar el estado del dron
UPDATE drones
SET estado = 'En Vuelo'
WHERE id_dron = p_id_dron;

-- Actualizar la misión (asignar dron, cambiar estado, registrar inicio)
UPDATE misiones
SET id_dron_asignado = p_id_dron,
    estado = 'En Progreso',
    fecha_inicio_real = CURRENT_TIMESTAMP
WHERE id_mision = p_id_mision;

END;
$$ LANGUAGE plpgsql;