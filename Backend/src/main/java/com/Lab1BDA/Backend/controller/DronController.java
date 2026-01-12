package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.model.Dron;
import com.Lab1BDA.Backend.service.DronService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drones")
@CrossOrigin(origins = "http://localhost:3000")
public class DronController {

    private final DronService dronService;

    @Autowired
    public DronController(DronService dronService) {
        this.dronService = dronService;
    }

    /**
     * Endpoint para crear un nuevo dron.
     * Se accede vía POST http://localhost:8081/api/drones
     * @param dronRequestDTO Los datos del nuevo dron en el cuerpo de la petición.
     * @return ResponseEntity con el dron creado y estado 201 (Created).
     */
    @PostMapping("/crear")
    public ResponseEntity<Dron> crearDron(@RequestBody DronRequestDTO dronRequestDTO) {
        Dron dronCreado = dronService.crearDron(dronRequestDTO);

        // Devolvemos 201 Created y la URI del nuevo recurso
        URI location = URI.create("/api/drones/" + dronCreado.getIdDron());
        return ResponseEntity.created(location).body(dronCreado);
    }

    /**
     * Endpoint para actualizar un dron existente.
     * Se accede vía PUT http://localhost:8081/api/drones/1
     * @param id El ID del dron a actualizar (desde la URL).
     * @param dronRequestDTO Los nuevos datos (desde el cuerpo de la petición).
     * @return ResponseEntity con el dron actualizado y estado 200 (OK).
     */
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<Dron> actualizarDron(@PathVariable("id") Long id, @RequestBody DronRequestDTO dronRequestDTO) {
        Dron dronActualizado = dronService.actualizarDron(id, dronRequestDTO);
        return ResponseEntity.ok(dronActualizado);
    }
    /**
     * Endpoint para eliminar un dron.
     * Se accede vía DELETE http://localhost:8081/api/drones/1
     * @param id El ID del dron a eliminar.
     * @return ResponseEntity con estado 204 (No Content).
     */
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarDron(@PathVariable("id") Long id) {
        dronService.eliminarDron(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }


    /**
     * Endpoint para obtener la lista de todos los drones.
     * Se accede vía GET http://localhost:8081/api/drones
     * @return ResponseEntity con la lista de drones y estado 200 (OK).
     */
    @GetMapping
    public ResponseEntity<List<Dron>> listarTodosLosDrones() {
        List<Dron> drones = dronService.getTodosLosDrones();
        return ResponseEntity.ok(drones);
    }

    /**
     * Endpoint para obtener un dron específico por su ID.
     * Se accede vía GET http://localhost:8081/api/drones/1
     * @param id El ID del dron pasado en la URL.
     * @return ResponseEntity con el Dron y estado 200 (OK).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dron> obtenerDronPorId(@PathVariable("id") Long id) {
        Dron dron = dronService.getDronPorId(id);
        return ResponseEntity.ok(dron);
    }

    /**
     * Endpoint para ejecutar la actualización masiva de mantenimiento (Requisito #7).
     * Llama al procedimiento almacenado para actualizar drones de un modelo
     * a 'En Mantenimiento' si superan las 100 horas de vuelo.
     * Se accede vía POST /api/drones/mantenimiento/modelo/{idModelo}
     * * @param idModelo El ID del modelo (desde la URL).
     * @return Un JSON con un mensaje y el conteo de drones actualizados.
     */
    @PostMapping("/mantenimiento/modelo/{idModelo}")
    public ResponseEntity<?> actualizarMantenimientoPorModelo(@PathVariable("idModelo") Long idModelo) {

        int dronesActualizados = dronService.solicitarMantenimientoPorModelo(idModelo);

        // Creamos una respuesta JSON simple usando un Map
        Map<String, Object> response = Map.of(
                "mensaje", "Ejecución de mantenimiento completada.",
                "dronesActualizados", dronesActualizados
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para el Reporte #8: Drones Inactivos.
     * Obtiene drones que no han volado en los últimos 30 días.
     * Se accede vía GET /api/drones/reportes/inactivos
     */
    @GetMapping("/reportes/inactivos")
    public ResponseEntity<List<DronInactivoDTO>> getReporteDronesInactivos() {
        List<DronInactivoDTO> reporte = dronService.getDronesInactivos();
        return ResponseEntity.ok(reporte);
    }


    /**
     * Endpoint para el Reporte #1: Análisis de Duración de Vuelo.
     * Obtiene el tiempo total de vuelo en horas por modelo en el último mes.
     * Se accede vía GET /api/drones/reportes/duracion-vuelo
     */
    @GetMapping("/reportes/duracion-vuelo")
    public ResponseEntity<List<DuracionVueloDTO>> getReporteDuracionVuelo() {
        List<DuracionVueloDTO> reporte = dronService.getReporteDuracionVuelo();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Endpoint para el Reporte #2: Drones con Fallos Recurrentes.
     * Obtiene los 5 drones con más misiones fallidas.
     * Se accede vía GET /api/drones/reportes/fallos
     */
    @GetMapping("/reportes/fallos")
    public ResponseEntity<List<DronFalloDTO>> getReporteDronesConFallos() {
        List<DronFalloDTO> reporte = dronService.getReporteDronesConFallos();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Endpoint para el Reporte #9: Análisis Geográfico de Puntos de Interés.
     * Encuentra los 5 drones más cercanos a un punto (lat, lon) en el último mes.
     * Se accede vía GET /api/drones/reportes/cercanos?lat=...&lon=...
     * @param lat Latitud del punto de interés.
     * @param lon Longitud del punto de interés.
     */
    @GetMapping("/reportes/cercanos")
    public ResponseEntity<List<AnalisisGeograficoDTO>> getReporteAnalisisGeografico(
            @RequestParam double lat,
            @RequestParam double lon) {

        List<AnalisisGeograficoDTO> reporte = dronService.getReporteAnalisisGeografico(lat, lon);
        return ResponseEntity.ok(reporte);
    }
    /**
     * Endpoint para obtener reporte de misiones de cada dron (misiones cumplidas, fallidas y tiempo de vuelo).
     * Acceso: GET /api/drones/reportes/global
     */
    @GetMapping("/reportes/global")
    public ResponseEntity<List<ReporteDesempenoDronDTO>> getReporteGlobal() {
        return ResponseEntity.ok(dronService.getReporteDesempenoGlobal());
    }

    /**
     * Endpoint para obtener 5 drones cercanos a cierto punto.
     * @param lat Latitud del punto consultado.
     * @param lon Longitud del punto consultado.
     * @return
     */
    @GetMapping("/cercanos/{lat}/{lon}")
    public ResponseEntity<List<AnalisisGeograficoDTO>> getDronesCercanos(@PathVariable double lat, @PathVariable double lon) {
        return ResponseEntity.ok(dronService.getReporteAnalisisGeografico(lat, lon));
    }

}