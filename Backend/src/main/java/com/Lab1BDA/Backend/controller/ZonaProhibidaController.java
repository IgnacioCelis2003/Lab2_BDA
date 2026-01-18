package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.dto.ZonaProhibidaDTO;
import com.Lab1BDA.Backend.model.ZonaProhibida;
import com.Lab1BDA.Backend.service.ZonaProhibidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zonas")
@CrossOrigin(origins = "http://localhost:3000")
public class ZonaProhibidaController {

    @Autowired
    private ZonaProhibidaService zonaService;
    @Autowired
    private ZonaProhibidaService zonaProhibidaService;

    @GetMapping("/listar")
    public List<ZonaProhibidaDTO> listarZonas() {
        return zonaService.listarTodas();
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crearZona(@RequestBody Map<String, String> body) {
        // El controller solo extrae datos y llama al servicio
        try {
            ZonaProhibida creada = zonaService.crearZona(body.get("nombre"), body.get("wkt"));
            return ResponseEntity.ok(creada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint para verificar si una ruta infringe zonas (útil para planificadores)
    @PostMapping("/verificar-ruta")
    public ResponseEntity<Map<String, Object>> verificarRuta(@RequestBody Map<String, String> body) {
        List<String> zonas = zonaService.verificarInfraccion(body.get("rutaWkt"));
        return ResponseEntity.ok(Map.of(
                "infringe", !zonas.isEmpty(),
                "zonasAfectadas", zonas
        ));
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Long id) {
        zonaProhibidaService.eliminarZona(id);
        return ResponseEntity.noContent().build();
    }
}