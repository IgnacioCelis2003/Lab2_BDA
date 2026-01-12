package com.Lab1BDA.Backend.controller;

import com.Lab1BDA.Backend.model.ModeloDron;
import com.Lab1BDA.Backend.service.ModeloDronService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/modelos")
@CrossOrigin(origins = "http://localhost:3000")
public class ModeloDronController {

    @Autowired
    private ModeloDronService modeloDronService;

    @GetMapping
    public ResponseEntity<List<ModeloDron>> listarModelos() {
        List<ModeloDron> modelos = modeloDronService.getTodosModelos();
        return ResponseEntity.ok(modelos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModeloDron> obtenerModelo(@PathVariable("id") Long id) {
        ModeloDron modelo = modeloDronService.getModeloPorId(id);
        return ResponseEntity.ok(modelo);
    }

    @PostMapping("/crear")
    public ResponseEntity<ModeloDron> crearModelo(@RequestBody ModeloDron modelo) {
        ModeloDron creado = modeloDronService.crearModelo(modelo);
        URI location = URI.create("/api/modelos/" + creado.getIdModelo());
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<ModeloDron> actualizarModelo(@PathVariable Long id, @RequestBody ModeloDron modelo) {
        ModeloDron actualizado = modeloDronService.actualizarModelo(id, modelo);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarModelo(@PathVariable Long id) {
        modeloDronService.eliminarModelo(id);
        return ResponseEntity.noContent().build();
    }
}
