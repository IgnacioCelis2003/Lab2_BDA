package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.exception.ResourceNotFoundException;
import com.Lab1BDA.Backend.model.Dron;
import com.Lab1BDA.Backend.repository.DronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DronService {

    @Autowired
    private DronRepository dronRepository;



    /**
     * Crea un nuevo dron.
     * @param dronRequestDTO Los datos del dron a crear.
     * @return El dron creado, incluyendo su ID.
     */
    public Dron crearDron(DronRequestDTO dronRequestDTO) {
        Dron dron = new Dron();
        dron.setIdModelo(dronRequestDTO.idModelo());

        // Lógica de negocio: si el DTO no especifica estado, por defecto es 'Disponible'
        if (dronRequestDTO.estado() == null || dronRequestDTO.estado().isEmpty()) {
            dron.setEstado("Disponible");
        } else {
            dron.setEstado(dronRequestDTO.estado());
        }

        return dronRepository.save(dron);
    }

    /**
     * Actualiza un dron existente.
     * @param id El ID del dron a actualizar.
     * @param dronRequestDTO Los nuevos datos para el dron.
     * @return El dron actualizado.
     * @throws ResourceNotFoundException si el dron no existe.
     */
    public Dron actualizarDron(Long id, DronRequestDTO dronRequestDTO) {
        // 1. Verificar si el dron existe (getDronPorId ya lanza 404 si no existe)
        Dron dronExistente = getDronPorId(id);

        // 2. Actualizar los campos
        dronExistente.setIdModelo(dronRequestDTO.idModelo());
        dronExistente.setEstado(dronRequestDTO.estado());

        // 3. Guardar los cambios
        return dronRepository.update(dronExistente);
    }

    /**
     * Elimina un dron por su ID.
     * @param id El ID del dron a eliminar.
     * @throws ResourceNotFoundException si el dron no existe.
     */
    public void eliminarDron(Long id) {
        // 1. Verificar si el dron existe (esto lanzará 404 si no)
        getDronPorId(id);

        // 2. Si existe, eliminarlo
        dronRepository.deleteById(id);
    }

    /**
     * Obtiene la lista de todos los drones.
     * @return Lista de Dron
     */
    public List<Dron> getTodosLosDrones() {
        return dronRepository.findAll();
    }

    /**
     * Obtiene un dron específico por su ID.
     * @param id El ID del dron.
     * @return El objeto Dron si se encuentra.
     * @throws ResourceNotFoundException si el dron no existe.
     */
    public Dron getDronPorId(Long id) {
        return dronRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dron no encontrado con id: " + id));
    }

    /**
     * Ejecuta la lógica de negocio para poner en mantenimiento a los drones
     * de un modelo específico según sus horas de vuelo.
     * @param idModelo El ID del modelo a verificar.
     * @return El número de drones actualizados.
     */
    public int solicitarMantenimientoPorModelo(Long idModelo) {
        // La lógica compleja ya está en la Base de Datos (Stored Procedure).
        // El servicio solo se encarga de llamarla.
        return dronRepository.llamarActualizacionMantenimiento(idModelo);
    }

    /**
     * Obtiene el reporte de drones inactivos (Requisito #8).
     * @return Lista de DronInactivoDTO
     */
    public List<DronInactivoDTO> getDronesInactivos() {
        return dronRepository.findDronesInactivos();
    }

    /**
     * Obtiene el reporte de duración de vuelo por modelo (Requisito #1).
     * @return Lista de DuracionVueloDTO
     */
    public List<DuracionVueloDTO> getReporteDuracionVuelo() {
        return dronRepository.findDuracionVueloPorModeloUltimoMes();
    }

    /**
     * Obtiene el reporte de drones con más fallos (Requisito #2).
     * @return Lista de DronFalloDTO
     */
    public List<DronFalloDTO> getReporteDronesConFallos() {
        return dronRepository.findDronesConMasFallos();
    }

    /**
     * Obtiene el reporte de análisis geográfico (Requisito #9).
     * @param latitud La latitud del punto de interés.
     * @param longitud La longitud del punto de interés.
     * @return Lista de AnalisisGeograficoDTO
     */
    public List<AnalisisGeograficoDTO> getReporteAnalisisGeografico(double latitud, double longitud) {
        return dronRepository.findDronesCercanosPunto(longitud, latitud);
    }
    public List<ReporteDesempenoDronDTO> getReporteDesempenoGlobal() {
        return dronRepository.findReporteDesempenoGlobal();
    }
}