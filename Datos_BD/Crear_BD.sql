-- ----------------------------------------------------------------
-- 1. ACTIVACIÓN DE EXTENSIONES
-- ----------------------------------------------------------------

-- Activa PostGIS en esta base de datos
-- (Requiere que PostGIS esté instalado en el servidor PostgreSQL)
CREATE EXTENSION IF NOT EXISTS postgis;

-- ----------------------------------------------------------------
-- 2. CREACIÓN DE TIPOS ENUM
-- ----------------------------------------------------------------

-- Usamos "IF NOT EXISTS" para que el script no falle si se corre más de una vez
CREATE TYPE estado_dron AS ENUM ('Disponible', 'En Vuelo', 'En Mantenimiento');
CREATE TYPE estado_mision AS ENUM ('Pendiente', 'En Progreso', 'Completada', 'Fallida');
CREATE TYPE rol_usuario AS ENUM ('Operador', 'Administrador');

-- ----------------------------------------------------------------
-- 3. CREACIÓN DE TABLAS
-- ----------------------------------------------------------------

-- Tabla para los modelos de los drones
CREATE TABLE IF NOT EXISTS modelos_dron (
    id_modelo SERIAL PRIMARY KEY,
    nombre_modelo VARCHAR(100) NOT NULL,
    fabricante VARCHAR(100),
    capacidad_carga_kg NUMERIC(5, 2),
    autonomia_minutos INT,
    velocidad_promedio_kmh NUMERIC(5, 2)
);

-- Tabla de Drones
CREATE TABLE IF NOT EXISTS drones (
    id_dron SERIAL PRIMARY KEY,
    id_modelo INT NOT NULL,
    estado estado_dron NOT NULL DEFAULT 'Disponible',
    CONSTRAINT fk_modelo
        FOREIGN KEY(id_modelo) 
        REFERENCES modelos_dron(id_modelo)
);

-- Tabla de Usuarios (Operadores y Administradores)
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol rol_usuario NOT NULL DEFAULT 'Operador'
);

-- Tabla para los tipos de misión
CREATE TABLE IF NOT EXISTS tipos_mision (
    id_tipo_mision SERIAL PRIMARY KEY,
    nombre_tipo VARCHAR(100) UNIQUE NOT NULL -- Ej: 'Entrega', 'Inspección', 'Vigilancia'
);

-- Tabla de Misiones 
CREATE TABLE IF NOT EXISTS misiones (
    id_mision SERIAL PRIMARY KEY,
    id_dron_asignado INT,
    id_tipo_mision INT NOT NULL,
    id_operador_creador INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_planificada TIMESTAMP,
    fecha_fin_planificada TIMESTAMP,
    fecha_inicio_real TIMESTAMP,
    fecha_fin_real TIMESTAMP,
    estado estado_mision NOT NULL DEFAULT 'Pendiente',
    
    -- LINESTRINGZ (La Z indica 3D)
    ruta GEOGRAPHY(LINESTRINGZ, 4326), 
    
    -- Indice para rastrear el progreso en la ruta
    ultimo_indice_ruta INT DEFAULT 0, 

    CONSTRAINT fk_dron FOREIGN KEY(id_dron_asignado) REFERENCES drones(id_dron),
    CONSTRAINT fk_tipo_mision FOREIGN KEY(id_tipo_mision) REFERENCES tipos_mision(id_tipo_mision),
    CONSTRAINT fk_operador FOREIGN KEY(id_operador_creador) REFERENCES usuarios(id_usuario)
);

-- Tabla de Registro de Vuelo (Telemetría) 
CREATE TABLE IF NOT EXISTS registro_vuelo (
    id_registro_vuelo BIGSERIAL PRIMARY KEY,
    id_mision INT NOT NULL,
    "timestamp" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- POINTZ incluye Lat, Lon y Altura
    coordenadas GEOGRAPHY(POINTZ, 4326) NOT NULL,
    
    -- Mantenemos esto para facilitar lecturas simples para el frontend
    altitud_msnm NUMERIC(7, 2), 
    
    velocidad_kmh NUMERIC(5, 2),
    nivel_bateria_porcentaje NUMERIC(5, 2) NOT NULL,
    
    CONSTRAINT fk_mision 
        FOREIGN KEY(id_mision) 
        REFERENCES misiones(id_mision)
        ON DELETE CASCADE
);

-- Tabla de Puntos de Interés
CREATE TABLE puntos_interes (
    poi_id SERIAL PRIMARY KEY,
    nombre VARCHAR(100),
    descripcion TEXT,
    -- GEOMETRY(POINTZ, SRID). 4326 es WGS84 (Lat/Lon)
    ubicacion GEOMETRY(POINTZ, 4326) NOT NULL
);

CREATE TABLE zonas_prohibidas (
    zona_id SERIAL PRIMARY KEY,
    nombre VARCHAR(100),
    -- Polígono 2D que define el área en el suelo
    area GEOMETRY(POLYGON, 4326) NOT NULL
);



-- ----------------------------------------------------------------
-- 4. CREACIÓN DE ÍNDICES
-- ----------------------------------------------------------------

-- Índices estándar para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_drones_estado ON drones(estado);
CREATE INDEX IF NOT EXISTS idx_misiones_estado ON misiones(estado);
CREATE INDEX IF NOT EXISTS idx_misiones_tipo ON misiones(id_tipo_mision);
CREATE INDEX IF NOT EXISTS idx_registro_vuelo_mision_time ON registro_vuelo(id_mision, "timestamp" DESC);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

-- Índice ESPACIAL (GIST): Fundamental para búsquedas rápidas de coordenadas
-- Esto es lo que permite que la consulta 9 sea eficiente 
CREATE INDEX IF NOT EXISTS idx_registro_vuelo_coordenadas ON registro_vuelo USING GIST (coordenadas);

CREATE INDEX IF NOT EXISTS idx_poi_geom ON puntos_interes USING GIST (ubicacion);
CREATE INDEX IF NOT EXISTS idx_zonas_area ON zonas_prohibidas USING GIST (area);