package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.exception.ResourceNotFoundException;
import com.Lab1BDA.Backend.model.ModeloDron;
import com.Lab1BDA.Backend.repository.ModeloDronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModeloDronService {

    @Autowired
    private ModeloDronRepository modeloDronRepository;

    public List<ModeloDron> getTodosModelos() {
        return modeloDronRepository.findAll();
    }

    public ModeloDron getModeloPorId(Long id) {
        return modeloDronRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de dron no encontrado con id: " + id));
    }

    public ModeloDron crearModelo(ModeloDron modelo) {
        // Ensure id is null so DB generates it
        modelo.setIdModelo(null);
        return modeloDronRepository.save(modelo);
    }

    public ModeloDron actualizarModelo(Long id, ModeloDron modelo) {
        // verify exists
        ModeloDron existente = getModeloPorId(id);

        // update fields
        existente.setNombreModelo(modelo.getNombreModelo());
        existente.setFabricante(modelo.getFabricante());
        existente.setCapacidadCargaKg(modelo.getCapacidadCargaKg());
        existente.setAutonomiaMinutos(modelo.getAutonomiaMinutos());
        existente.setVelocidadPromedioKmh(modelo.getVelocidadPromedioKmh());

        return modeloDronRepository.update(existente);
    }

    public void eliminarModelo(Long id) {
        // verify exists
        getModeloPorId(id);
        modeloDronRepository.deleteById(id);
    }
}

