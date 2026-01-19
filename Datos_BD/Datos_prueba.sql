--Limpieza de datos y reinicio de id
TRUNCATE TABLE registro_vuelo, misiones, zonas_prohibidas, puntos_interes, tipos_mision, usuarios, drones, modelos_dron RESTART IDENTITY CASCADE;
--EJECUTAR TABLA POR TABLA PARA EVITAR ERRORES DE LLAVES FORANEA
--Usuario de prueba (no para acceso)

INSERT INTO usuarios(nombre,email,contrasena_hash,rol) VALUES ('test','test','test','Operador');

-- Tipos de Mision

INSERT INTO tipos_mision(nombre_tipo) VALUES ('Entrega'),('Inspeccion'),('Vigilancia');

-- Modelos de Dron

INSERT INTO modelos_dron(nombre_modelo,fabricante,capacidad_carga_kg,autonomia_minutos,velocidad_promedio_kmh

)

VALUES
    ('Gamma',   'Stella', 5.20, 120, 55.0),
    ('Crux',    'Stella', 7.50,  80, 48.0),
    ('Lyra',    'Stella',10.00, 150, 45.0),
    ('Leo',     'Stella', 3.50, 360, 60.0),
    ('Basalt',  'Geo',    5.50,  80, 50.0),
    ('Granite', 'Geo',    3.60, 130, 58.0),
    ('Diorite', 'Geo',    7.50,  60, 42.0),
    ('Latite',  'Geo',    5.50,  80, 50.0),
    ('DJ1',     'Agra',   4.00, 100, 62.0),
    ('DJ2',     'Agra',   5.50,  85, 55.0),
    ('DJ3',     'Agra',   7.00,  60, 47.0),
    ('DJ4',     'Agra',  10.00,  60, 40.0);

-- Drones

INSERT INTO drones(id_modelo,estado) VALUES (1,'Disponible'),(1,'Disponible'),(1,'En Mantenimiento'),

(1,'Disponible'),

(2,'Disponible'),(2,'Disponible'),(2,'En Mantenimiento'),(2,'Disponible'),

(3,'Disponible'),(3,'Disponible'),(3,'En Mantenimiento'),(3,'Disponible'),

(4,'Disponible'),(4,'Disponible'),(4,'En Mantenimiento'),(4,'Disponible'),

(5,'Disponible'),(5,'Disponible'),(5,'En Mantenimiento'),(5,'Disponible'),

(6,'Disponible'),(6,'Disponible'),(6,'En Mantenimiento'),(6,'Disponible'),

(7,'Disponible'),(7,'Disponible'),(7,'En Mantenimiento'),(7,'Disponible'),

(8,'Disponible'),(8,'Disponible'),(8,'En Mantenimiento'),(8,'Disponible'),

(9,'Disponible'),(9,'Disponible'),(9,'En Mantenimiento'),(9,'Disponible'),

(10,'Disponible'),(10,'Disponible'),(10,'En Mantenimiento'),(10,'Disponible'),

(11,'Disponible'),(11,'Disponible'),(11,'En Mantenimiento'),(11,'Disponible'),

(12,'Disponible'),(12,'Disponible'),(12,'En Mantenimiento'),(12,'Disponible');

--Zona prohibida de prueba
INSERT INTO zonas_prohibidas (nombre, area)VALUES (

'Zona Restringida Hospital',

ST_GeomFromText('POLYGON((-70.6620 -33.4550, -70.6600 -33.4550, -70.6600 -33.4570, -70.6620 -33.4570, -70.6620 -33.4550))', 4326)

);

--Punto de Interés (POI) - "Torre de Control"
INSERT INTO puntos_interes (nombre, descripcion, ubicacion)VALUES (

'Torre de Control',

'Antena principal de comunicaciones',

ST_SetSRID(ST_MakePoint(-70.65000, -33.45000, 10.0), 4326)

); 

--Creación de misiones

-- La ruta planificada une los puntos que insertaremos en la telemetría.
--Mision 1 mas menos 500 metros
INSERT INTO misiones (

    id_dron_asignado, id_tipo_mision, id_operador_creador, 

    fecha_creacion, estado, ruta

) VALUES (

    1, 1, 1, 

    '2026-01-20 08:00:00', 'En Progreso',

    ST_GeographyFromText('LINESTRINGZ(-70.65538 -33.45000 100.0, -70.65400 -33.45200 105.0, -70.65200 -33.45400 110.0)')

);
--Misión 2: Trayectoria CERCANA (~100 metros del POI)
INSERT INTO misiones (id_dron_asignado, id_tipo_mision, id_operador_creador, fecha_creacion, estado, ruta) 

VALUES (1, 2, 1, '2026-01-20 10:00:00', 'Completada',
    ST_GeographyFromText('LINESTRINGZ(-70.65110 -33.45000 100.0, -70.65150 -33.45100 105.0, -70.65200 -33.45200 110.0)')

);

--Misión 3: Trayectoria MEDIA (~500 metros del POI)

INSERT INTO misiones (id_dron_asignado, id_tipo_mision, id_operador_creador, fecha_creacion, estado, ruta) 

VALUES (1, 2, 1, '2026-01-20 11:00:00', 'Completada',
	ST_GeographyFromText('LINESTRINGZ(-70.65538 -33.45000 100.0, -70.65600 -33.45100 105.0, -70.65700 -33.45200 110.0)')
);

--Misión 4: Trayectoria LEJANA (~1000 metros / 1km del POI)
-- Volamos por el Norte para alejarnos del POI y de la zona prohibida del Sur.
INSERT INTO misiones (id_dron_asignado, id_tipo_mision, id_operador_creador, fecha_creacion, estado, ruta) 
VALUES (1, 2, 1, '2026-01-20 12:00:00', 'Completada',
    ST_GeographyFromText('LINESTRINGZ(-70.66076 -33.44800 100.0, -70.66100 -33.44700 105.0, -70.66150 -33.44600 110.0)')
);


--Insertar Registros de vuelo


--Mision 1
INSERT INTO registro_vuelo (id_mision, "timestamp", coordenadas, altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje) 
VALUES
(1, '2026-01-20 09:00:00', ST_GeographyFromText('POINTZ(-70.65538 -33.45000 100.0)'), 100.0, 40.0, 95.0),
(1, '2026-01-20 09:02:00', ST_GeographyFromText('POINTZ(-70.65400 -33.45200 105.0)'), 105.0, 42.0, 90.0),
(1, '2026-01-20 09:04:00', ST_GeographyFromText('POINTZ(-70.65200 -33.45400 110.0)'), 110.0, 45.0, 85.0);

--Mision 2
INSERT INTO registro_vuelo (id_mision, "timestamp", coordenadas, altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje) VALUES
(2, '2026-01-20 10:05:00', ST_GeographyFromText('POINTZ(-70.65110 -33.45000 100.0)'), 100.0, 30.0, 98.0), -- Punto a ~100m
(2, '2026-01-20 10:07:00', ST_GeographyFromText('POINTZ(-70.65150 -33.45100 105.0)'), 105.0, 32.0, 95.0),
(2, '2026-01-20 10:09:00', ST_GeographyFromText('POINTZ(-70.65200 -33.45200 110.0)'), 110.0, 35.0, 92.0);

--Mision 3
INSERT INTO registro_vuelo (id_mision, "timestamp", coordenadas, altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje) VALUES
(3, '2026-01-20 11:05:00', ST_GeographyFromText('POINTZ(-70.65538 -33.45000 100.0)'), 100.0, 40.0, 90.0), -- Punto a ~500m
(3, '2026-01-20 11:07:00', ST_GeographyFromText('POINTZ(-70.65600 -33.45100 105.0)'), 105.0, 42.0, 85.0),
(3, '2026-01-20 11:09:00', ST_GeographyFromText('POINTZ(-70.65700 -33.45200 110.0)'), 110.0, 45.0, 80.0);

--Mision 4
INSERT INTO registro_vuelo (id_mision, "timestamp", coordenadas, altitud_msnm, velocidad_kmh, nivel_bateria_porcentaje) VALUES
(4, '2026-01-20 12:05:00', ST_GeographyFromText('POINTZ(-70.66076 -33.44800 100.0)'), 100.0, 50.0, 75.0), -- Punto a ~1000m
(4, '2026-01-20 12:07:00', ST_GeographyFromText('POINTZ(-70.66100 -33.44700 105.0)'), 105.0, 52.0, 70.0),
(4, '2026-01-20 12:09:00', ST_GeographyFromText('POINTZ(-70.66150 -33.44600 110.0)'), 110.0, 55.0, 65.0);


