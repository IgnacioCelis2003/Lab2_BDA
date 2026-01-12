package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.exception.ResourceNotFoundException;
import com.Lab1BDA.Backend.model.TipoMision;
import com.Lab1BDA.Backend.repository.TipoMisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoMisionService {

    @Autowired
    private TipoMisionRepository tipoMisionRepository;

    public List<TipoMision> getTodosTipos() {
        return tipoMisionRepository.findAll();
    }

    public TipoMision getTipoPorId(Long id) {
        return tipoMisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de misión no encontrado con id: " + id));
    }

    public TipoMision crearTipo(TipoMision tipoMision) {
        // Ensure id is null so DB generates it
        tipoMision.setIdTipoMision(null);
        return tipoMisionRepository.save(tipoMision);
    }

    public TipoMision actualizarTipo(Long id, TipoMision tipoMision) {
        // verify exists
        TipoMision existente = getTipoPorId(id);

        // update fields
        existente.setNombreTipo(tipoMision.getNombreTipo());

        return tipoMisionRepository.update(existente);
    }

    public void eliminarTipo(Long id) {
        // verify exists
        getTipoPorId(id);
        tipoMisionRepository.deleteById(id);
    }
}

