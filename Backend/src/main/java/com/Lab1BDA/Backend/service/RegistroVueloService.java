package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.dto.RegistroVueloRequestDTO;
import com.Lab1BDA.Backend.dto.UbicacionDTO;
import com.Lab1BDA.Backend.model.Dron;
import com.Lab1BDA.Backend.model.Mision;
import com.Lab1BDA.Backend.model.ModeloDron;
import com.Lab1BDA.Backend.model.RegistroVuelo;
import com.Lab1BDA.Backend.repository.DronRepository;
import com.Lab1BDA.Backend.repository.MisionRepository;
import com.Lab1BDA.Backend.repository.ModeloDronRepository;
import com.Lab1BDA.Backend.repository.RegistroVueloRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RegistroVueloService {

    @Autowired
    private RegistroVueloRepository registroVueloRepository;

    private final WKTReader wktReader = new WKTReader(); // Para leer el WKT del DTO
    @Autowired
    private MisionRepository misionRepository;
    @Autowired
    private DronRepository dronRepository;
    @Autowired
    private ModeloDronRepository modeloDronRepository;


    /**
     * Registra un nuevo punto de telemetría.
     * @param dto El DTO con los datos de telemetría.
     * @return El registro de vuelo guardado.
     */
    public RegistroVuelo registrarTelemetria(RegistroVueloRequestDTO dto) {
        RegistroVuelo registro = new RegistroVuelo();
        registro.setIdMision(dto.idMision());
        registro.setAltitudMsnm(dto.altitudMsnm());
        registro.setVelocidadKmh(dto.velocidadKmh());
        registro.setNivelBateriaPorcentaje(dto.nivelBateriaPorcentaje());

        // Si el DTO no trae timestamp, usamos el actual
        registro.setTimestamp(dto.timestamp() != null ? dto.timestamp() : LocalDateTime.now());

        // Convertir el String WKT (ej: "POINT(1 2)") en un objeto Point
        try {
            Geometry geom = wktReader.read(dto.coordenadasWKT());
            if (geom instanceof Point) {
                registro.setCoordenadas((Point) geom);
            } else {
                throw new IllegalArgumentException("El WKT proporcionado no es un POINT");
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Formato WKT de coordenadas inválido", e);
        }

        return registroVueloRepository.save(registro);
    }

    /**
     * Obtiene todos los datos de telemetría para una misión.
     * @param idMision El ID de la misión.
     * @return Lista de registros de telemetría.
     */
    public List<RegistroVuelo> getTelemetriaPorMision(Long idMision) {
        return registroVueloRepository.findByMisionId(idMision);
    }


    /**
     * Pasa un registro de vuelo a UbicacionDTO, para mostrarlo en el mapa.
     * @param registro
     * @return
     */
    public UbicacionDTO registroToUbicacion(RegistroVuelo registro) {
        // Correccion de coordenadas
        double latitud = registro.getCoordenadas().getY();
        double longitud = registro.getCoordenadas().getX();

        return new UbicacionDTO(
                registro.getIdMision(),
                registro.getTimestamp(),
                latitud,
                longitud,
                registro.getNivelBateriaPorcentaje()
        );
    }

    /**
     * Obtiene las ubicaciones de todos los drones activos en un determinado momento.
     * Se ejecuta automáticamente cada 5 segundos.
     */
    @Scheduled(fixedRate = 5000)
    public void autoMonitoreo(){
        // Obtener el registro más reciente de cada misión en estado "En Progreso"
        List<RegistroVuelo> registros = registroVueloRepository.findLatestByMisionWithActiveStatus();
        actualizarMonitoreo(registros, 5);
    }

    public List<UbicacionDTO> getMonitoreo(){
        // Obtener el registro más reciente de cada misión en estado "En Progreso"
        List<RegistroVuelo> registros = registroVueloRepository.findLatestByMisionWithActiveStatus();
        List<UbicacionDTO> ubicaciones = new ArrayList<>();
        for (RegistroVuelo registro : registros) {
            ubicaciones.add(registroToUbicacion(registro));
        }
        return ubicaciones;
    }

    /**
     * Hace avanzar a los drones activos en el espacio y el tiempo
     * @param registros Registros que se quieren avanzar.
     * @param segundos  Cuántos segundos se quiere avanzar.
     * @return Registros de los drones tras avanzar.
     */
    public List<RegistroVuelo> actualizarMonitoreo(List<RegistroVuelo> registros, long segundos) {
        List<RegistroVuelo> registrosActualizados = new ArrayList<>();
        // Para cada mision en proceso se obtiene su ultimo punto de telemetria
        for (RegistroVuelo registro : registros) {
            RegistroVuelo nuevoRegistro = registro;
            Mision mision = misionRepository.findById(registro.getIdMision())
                    .orElseThrow(() -> new IllegalStateException("Misión no encontrada"));
            Dron dron = dronRepository.findById(mision.getIdDronAsignado())
                    .orElseThrow(() -> new IllegalStateException("Dron no encontrado"));
            ModeloDron modelo = modeloDronRepository.findById(dron.getIdModelo())
                    .orElseThrow(() -> new IllegalStateException("Modelo de dron no encontrado"));
            double velocidad = modelo.getVelocidadPromedioKmh();
            Point destino = mision.getRuta().getEndPoint();

            double lon = registro.getCoordenadas().getX();
            double lat = registro.getCoordenadas().getY();
            // Cálculo de la nueva posición del dron
            // X->Longitud | Y->Latitud
            double dx = destino.getX() - lon;
            double dy = destino.getY() - lat;
            double metrosPorGradoLat = 111_320.0;
            double metrosPorGradoLon = 111_320.0 * Math.cos(Math.toRadians(lat));
            double distanciaRestante = Math.sqrt(
                    Math.pow(dx * metrosPorGradoLon, 2) +
                            Math.pow(dy * metrosPorGradoLat, 2)
            );
            double distanciaAvance = (velocidad / 3.6) * segundos;
            double nuevaLat;
            double nuevaLon;
            // Verificar que el dron no se pasó de su destino
            if (distanciaAvance >= distanciaRestante) {
                // Llegó o se pasó
                nuevaLat = destino.getY();
                nuevaLon = destino.getX();
                mision.setEstado("Completada");
                dron.setEstado("Disponible");
                dronRepository.update(dron);
                misionRepository.update(mision);
                registroVueloRepository.save(nuevoRegistro);
            } else {
                double modulo = Math.sqrt(dx*dx + dy*dy);
                double dirX = dx / modulo;
                double dirY = dy / modulo;
                double deltaLat = (dirY * distanciaAvance) / metrosPorGradoLat;
                double deltaLon = (dirX * distanciaAvance) / metrosPorGradoLon;
                nuevaLat = lat + deltaLat;
                nuevaLon = lon + deltaLon;
            }
            // Pasar las coordenadas a Point y agregar tiempo
            GeometryFactory geometryFactory = new GeometryFactory();
            Point nuevasCoordenadas = geometryFactory.createPoint(new Coordinate(nuevaLon, nuevaLat));
            nuevoRegistro.setCoordenadas(nuevasCoordenadas);
            nuevoRegistro.setTimestamp(LocalDateTime.now());
            registroVueloRepository.save(nuevoRegistro);
            registrosActualizados.add(nuevoRegistro);
        }
        return registrosActualizados;
    }

//    //Registro por id de dron
//    public List<RegistroVuelo> getRegistroVuelo(long id) {
//        //  obtener mision con id de dron
//        long idMision = misionRepository.findByDronId(id).getId().orElseThrow();
//        return registroVueloRepository.findByMisionId(idMision);
//    }
}