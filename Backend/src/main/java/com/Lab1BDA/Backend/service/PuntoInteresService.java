package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.exception.ResourceNotFoundException;
import com.Lab1BDA.Backend.model.PuntoInteres;
import com.Lab1BDA.Backend.repository.PuntoInteresRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PuntoInteresService {

    @Autowired
    private PuntoInteresRepository puntoInteresRepository;

    // SRID 4326 es el estándar WGS84 (Latitud/Longitud) usado en tu BD
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final WKTReader wktReader = new WKTReader(geometryFactory);

    public List<PuntoInteres> listarTodos() {
        return puntoInteresRepository.findAll();
    }

    public PuntoInteres obtenerPorId(Long id) {
        return puntoInteresRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Punto de Interés no encontrado con id: " + id));
    }

    /**
     * Crea un punto de interés recibiendo latitud, longitud y altitud.
     */
    public PuntoInteres crearPunto(String nombre, String descripcion, double lat, double lon, double altitud) {
        PuntoInteres poi = new PuntoInteres();
        poi.setNombre(nombre);
        poi.setDescripcion(descripcion);

        // Creamos la coordenada con Z (Altitud) para POINTZ
        Coordinate coord = new Coordinate(lon, lat, altitud);
        Point punto = geometryFactory.createPoint(coord);

        poi.setUbicacion(punto);
        return puntoInteresRepository.save(poi);
    }

    /**
     * Alternativa: Crear punto mediante una cadena WKT (ej: "POINT(long lat alt)")
     */
    public PuntoInteres crearPuntoDesdeWKT(String nombre, String descripcion, String wkt) {
        try {
            Point punto = (Point) wktReader.read(wkt);
            punto.setSRID(4326);

            PuntoInteres poi = new PuntoInteres();
            poi.setNombre(nombre);
            poi.setDescripcion(descripcion);
            poi.setUbicacion(punto);

            return puntoInteresRepository.save(poi);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Formato WKT inválido: " + wkt);
        }
    }

    public PuntoInteres actualizarPunto(Long id, String nombre, String descripcion, Double lat, Double lon, Double alt) {
        PuntoInteres existente = obtenerPorId(id);

        existente.setNombre(nombre);
        existente.setDescripcion(descripcion);

        if (lat != null && lon != null) {
            Coordinate coord = new Coordinate(lon, lat, alt != null ? alt : 0.0);
            existente.setUbicacion(geometryFactory.createPoint(coord));
        }

        puntoInteresRepository.update(existente);
        return existente;
    }

    public void eliminarPunto(Long id) {
        if (puntoInteresRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("No se puede eliminar id: " + id);
        }
        puntoInteresRepository.deleteById(id);
    }

}