package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.model.PuntoInteres;
import com.Lab1BDA.Backend.service.PuntoInteresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/puntos-interes")
@CrossOrigin(origins = "http://localhost:3000")
public class PuntoInteresController {

    @Autowired
    private PuntoInteresService puntoInteresService;

    /**
     * Obtener todos los puntos de interés.
     * GET /api/puntos-interes
     */
    @GetMapping
    public ResponseEntity<List<PuntoInteres>> listar() {
        return ResponseEntity.ok(puntoInteresService.listarTodos());
    }

    /**
     * Obtener un punto específico por su id.
     * GET /api/puntos-interes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PuntoInteres> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(puntoInteresService.obtenerPorId(id));
    }

    /**
     * Crear un nuevo punto de interés.
     * POST /api/puntos-interes/crear
     */
    @PostMapping("/crear")
    public ResponseEntity<PuntoInteres> crear(@RequestBody Map<String, Object> payload) {
        String nombre = (String) payload.get("nombre");
        String descripcion = (String) payload.get("descripcion");

        // Extraemos coordenadas (lat, lon, alt) del JSON
        double lat = ((Number) payload.get("latitud")).doubleValue();
        double lon = ((Number) payload.get("longitud")).doubleValue();
        double alt = payload.containsKey("altitud") ? ((Number) payload.get("altitud")).doubleValue() : 0.0;

        PuntoInteres nuevo = puntoInteresService.crearPunto(nombre, descripcion, lat, lon, alt);

        URI location = URI.create("/api/puntos-interes/" + nuevo.getPoi_id());
        return ResponseEntity.created(location).body(nuevo);
    }

    /**
     * Actualizar un punto de interés.
     * PUT /api/puntos-interes/actualizar/{id}
     */
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<PuntoInteres> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String nombre = (String) payload.get("nombre");
        String descripcion = (String) payload.get("descripcion");

        Double lat = payload.containsKey("latitud") ? ((Number) payload.get("latitud")).doubleValue() : null;
        Double lon = payload.containsKey("longitud") ? ((Number) payload.get("longitud")).doubleValue() : null;
        Double alt = payload.containsKey("altitud") ? ((Number) payload.get("altitud")).doubleValue() : 0.0;

        PuntoInteres actualizado = puntoInteresService.actualizarPunto(id, nombre, descripcion, lat, lon, alt);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Eliminar un punto de interés.
     * DELETE /api/puntos-interes/eliminar/{id}
     */
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        puntoInteresService.eliminarPunto(id);
        return ResponseEntity.noContent().build();
    }

}