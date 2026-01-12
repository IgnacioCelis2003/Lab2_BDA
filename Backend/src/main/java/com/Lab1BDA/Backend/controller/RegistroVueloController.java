package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.dto.RegistroVueloRequestDTO;
import com.Lab1BDA.Backend.dto.UbicacionDTO;
import com.Lab1BDA.Backend.model.RegistroVuelo;
import com.Lab1BDA.Backend.service.RegistroVueloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetria")
@CrossOrigin(origins = "http://localhost:3000")
public class RegistroVueloController {

    @Autowired
    private RegistroVueloService registroVueloService;

    /**
     * Endpoint para que un dron registre un nuevo punto de telemetría.
     * Se accede vía POST /api/telemetria/registrar
     * @param dto El cuerpo del JSON con los datos de telemetría.
     * @return El registro de telemetría que fue guardado en la BD (con HTTP 201 Created).
     */
    @PostMapping("/registrar")
    public ResponseEntity<RegistroVuelo> registrarTelemetria(@RequestBody RegistroVueloRequestDTO dto) {

        RegistroVuelo registroGuardado = registroVueloService.registrarTelemetria(dto);

        // Devolvemos 201 Created, ya que se creó un nuevo recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(registroGuardado);
    }

    /**
     * Endpoint para obtener todo el historial de telemetría de una misión específica.
     * Se accede vía GET /api/telemetria/mision/1
     * @param idMision El ID de la misión (desde la URL).
     * @return Una lista de todos los registros de vuelo para esa misión.
     */
    @GetMapping("/mision/{idMision}")
    public ResponseEntity<List<RegistroVuelo>> getTelemetriaPorMision(@PathVariable Long idMision) {

        List<RegistroVuelo> telemetria = registroVueloService.getTelemetriaPorMision(idMision);
        return ResponseEntity.ok(telemetria);
    }

    @GetMapping("/monitoreo")
    public ResponseEntity<List<UbicacionDTO>> getMonitoreo() {
        List<UbicacionDTO> ubicaciones = registroVueloService.getMonitoreo();
        return ResponseEntity.ok(ubicaciones);
    }
}