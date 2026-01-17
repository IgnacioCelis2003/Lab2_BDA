package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.model.ZonaProhibida;
import com.Lab1BDA.Backend.repository.ZonaProhibidaRepository;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZonaProhibidaService {

    @Autowired
    private ZonaProhibidaRepository zonaRepository;

    private final WKTReader wktReader = new WKTReader();

    public List<ZonaProhibida> listarTodas() {
        return zonaRepository.findAll();
    }

    public ZonaProhibida crearZona(String nombre, String wkt) {
        try {
            Polygon polygon = (Polygon) wktReader.read(wkt);
            polygon.setSRID(4326); // Importante para PostGIS

            ZonaProhibida zona = new ZonaProhibida();
            zona.setNombre(nombre);
            zona.setArea(polygon);

            return zonaRepository.save(zona);
        } catch (ParseException e) {
            throw new IllegalArgumentException("WKT inválido para Polígono: " + wkt, e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("La geometría proporcionada no es un Polígono", e);
        }
    }

    /**
     * Lógica para el requisito de Infracción de Zonas Aéreas.
     * Verifica si una ruta planificada o un vuelo en curso pasa por zonas prohibidas.
     */
    public List<String> verificarInfraccion(String rutaWkt) {
        if (rutaWkt == null || rutaWkt.isEmpty()) {
            return List.of();
        }
        return zonaRepository.encontrarInfracciones(rutaWkt);
    }
}