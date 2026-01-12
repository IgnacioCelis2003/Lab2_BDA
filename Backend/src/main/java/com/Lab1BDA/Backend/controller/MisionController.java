package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.model.Mision;
import com.Lab1BDA.Backend.service.MisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de Misiones.
 * Todos los endpoints aquí están protegidos por JWT (definido en SecurityConfig).
 */
@RestController
@RequestMapping("/api/misiones")
@CrossOrigin(origins = "http://localhost:3000")
public class MisionController {

    @Autowired
    private MisionService misionService;

    /**
     * Endpoint para obtener todas las misiones.
     * GET /api/misiones
     */
    @GetMapping
    public ResponseEntity<List<Mision>> listarTodasLasMisiones() {
        return ResponseEntity.ok(misionService.getTodasLasMisiones());
    }

    /**
     * Endpoint para obtener una misión por su ID.
     * GET /api/misiones/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Mision> obtenerMisionPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(misionService.getMisionPorId(id));
    }

    /**
     * Endpoint para crear una nueva misión.
     * POST /api/misiones/crear
     *
     * @param misionDTO       Los datos de la misión (desde el cuerpo JSON).
     * @param authentication  Spring Security inyecta esto automáticamente
     *                        con los datos del usuario autenticado (del token).
     * @return La misión creada (con HTTP 201 Created).
     */
    @PostMapping("/crear")
    public ResponseEntity<Mision> crearMision(
            @RequestBody MisionRequestDTO misionDTO,
            Authentication authentication
    ) {
        // Email del usuario autenticado (username del JWT)
        String emailCreador = authentication.getName();

        Mision misionCreada = misionService.crearMision(misionDTO, emailCreador);

        URI location = URI.create("/api/misiones/" + misionCreada.getIdMision());
        return ResponseEntity.created(location).body(misionCreada);
    }

    /**
     * Endpoint para actualizar una misión existente.
     * PUT /api/misiones/actualizar/{id}
     */
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<Mision> actualizarMision(
            @PathVariable Long id,
            @RequestBody MisionRequestDTO misionDTO
    ) {
        Mision misionActualizada = misionService.actualizarMision(id, misionDTO);
        return ResponseEntity.ok(misionActualizada);
    }

    /**
     * Endpoint para eliminar una misión.
     * DELETE /api/misiones/eliminar/{id}
     */
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarMision(@PathVariable("id") Long id) {
        misionService.eliminarMision(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    /**
     * Endpoint para asignar una misión existente a un dron específico.
     * Llama al procedimiento almacenado.
     * Se accede vía POST /api/misiones/{idMision}/asignar/{idDron}
     *
     * @param idMision El ID de la misión (desde la URL).
     * @param idDron   El ID del dron (desde la URL).
     * @return ResponseEntity con estado 200 (OK) si tiene éxito.
     */
    @PostMapping("/{idMision}/asignar/{idDron}")
    public ResponseEntity<Void> asignarMision(
            @PathVariable("idMision") Long idMision,
            @PathVariable("idDron") Long idDron
    ) {
        misionService.asignarMisionADron(idMision, idDron);
        return ResponseEntity.ok().build();
    }

    /**
     * Genera una ruta óptima para una flota de drones.
     * Considera: Distancia (PostGIS), Disponibilidad y Autonomía v/s Velocidad del Dron.
     * Se accede vía POST /api/misiones/optimizar-ruta
     *
     * @param idMisiones Lista con los id de las misiones a optimizar
     * @return ResponseEntity con la ruta óptima.
     */
    @PostMapping("/optimizar-ruta")
    public ResponseEntity<RutaOptimaResponseDTO> optimizarRuta(@RequestBody List<Long> idMisiones) {
        RutaOptimaResponseDTO respuesta = misionService.generarRutaOptimaMultidron(idMisiones);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Endpoint para el Reporte #3: Desempeño por Tipo de Misión.
     * Compara misiones completadas por tipo para los 2 modelos de dron más usados.
     * Se accede vía GET /api/misiones/reportes/desempeno-tipo
     */
    @GetMapping("/reportes/desempeno-tipo")
    public ResponseEntity<List<DesempenoTipoMisionDTO>> getReporteDesempenoTipoMision() {
        List<DesempenoTipoMisionDTO> reporte = misionService.getReporteDesempenoTipoMision();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Reporte #4: Patrones de Consumo de Batería.
     * GET /api/misiones/reportes/consumo-bateria
     */
    @GetMapping("/reportes/consumo-bateria")
    public ResponseEntity<List<BateriaConsumoDTO>> getReporteConsumoBateria() {
        List<BateriaConsumoDTO> reporte =
                misionService.getReporteConsumoBateria();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Reporte #5: Análisis de Desempeño Mensual.
     * GET /api/misiones/reportes/desempeno-mensual
     */
    @GetMapping("/reportes/desempeno-mensual")
    public ResponseEntity<List<DesempenoMensualDTO>> getReporteDesempenoMensual() {
        List<DesempenoMensualDTO> reporte =
                misionService.getReporteDesempenoMensual();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Reporte #10: Resumen de Misiones por Tipo (Vista Materializada).
     * GET /api/misiones/reportes/resumen-tipo
     */
    @GetMapping("/reportes/resumen-tipo")
    public ResponseEntity<List<ResumenMisionTipoDTO>> getReporteResumenMisiones() {
        List<ResumenMisionTipoDTO> reporte =
                misionService.getReporteResumenMisiones();
        return ResponseEntity.ok(reporte);
    }

    /**
     * Iniciar misión
     * @param idMision ID de la misión a iniciar.
     * @return
     */
    @PostMapping("/iniciar/{idMision}")
    public ResponseEntity<String> iniciarMision(@PathVariable Long idMision) {
        misionService.iniciarMision(idMision);
        return ResponseEntity.ok("Misión iniciada.");
    }

    /**
     * Completar misión (cambia estado a "Completada" y refresca la vista materializada)
     * @param idMision ID de la misión a completar.
     * @return
     */
    @PostMapping("/completar/{idMision}")
    public ResponseEntity<String> completarMision(@PathVariable Long idMision) {
        misionService.completarMision(idMision);
        return ResponseEntity.ok("Misión completada y vista materializada actualizada.");
    }
}
