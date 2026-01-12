package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.model.TipoMision;
import com.Lab1BDA.Backend.service.TipoMisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-mision")
@CrossOrigin(origins = "http://localhost:3000")
public class TipoMisionController {

    @Autowired
    private TipoMisionService tipoMisionService;

    @GetMapping
    public ResponseEntity<List<TipoMision>> listarTipos() {
        List<TipoMision> tipos = tipoMisionService.getTodosTipos();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoMision> obtenerTipo(@PathVariable("id") Long id) {
        TipoMision tipo = tipoMisionService.getTipoPorId(id);
        return ResponseEntity.ok(tipo);
    }

    @PostMapping("/crear")
    public ResponseEntity<TipoMision> crearTipo(@RequestBody TipoMision tipoMision) {
        TipoMision creado = tipoMisionService.crearTipo(tipoMision);
        URI location = URI.create("/api/tipos-mision/" + creado.getIdTipoMision());
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<TipoMision> actualizarTipo(@PathVariable Long id, @RequestBody TipoMision tipoMision) {
        TipoMision actualizado = tipoMisionService.actualizarTipo(id, tipoMision);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Long id) {
        tipoMisionService.eliminarTipo(id);
        return ResponseEntity.noContent().build();
    }
}

