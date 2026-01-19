package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.dto.RegistroVueloRequestDTO;
import com.Lab1BDA.Backend.dto.UbicacionDTO;
import com.Lab1BDA.Backend.dto.VelocidadCalculadaDTO;
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
        registro.setTimestamp(dto.timestamp() != null ? dto.timestamp() : LocalDateTime.now());

        try {
            // 1. Leemos el WKT (Ej: "POINT(-70.6 33.4)") que suele ser 2D
            Geometry geom = wktReader.read(dto.coordenadasWKT());

            if (geom instanceof Point) {
                Point p2d = (Point) geom;

                // 2. CREAMOS EL PUNTO 3D REAL
                // Obtenemos Z del DTO, o asumimos 0.0 si es nulo
                double z = dto.altitudMsnm() != null ? dto.altitudMsnm() : 0.0;

                // Coordinate soporta (x, y, z)
                Coordinate coord3d = new Coordinate(p2d.getX(), p2d.getY(), z);

                GeometryFactory factory = new GeometryFactory();
                Point p3d = factory.createPoint(coord3d);
                p3d.setSRID(4326); // Importante para PostGIS

                registro.setCoordenadas(p3d);
            } else {
                throw new IllegalArgumentException("El WKT no es un POINT");
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error al leer coordenadas", e);
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

            // Obtener la altitud actual del registro
            double alt = registro.getCoordenadas().getCoordinate().getZ();
            if (Double.isNaN(alt)) {
                alt = registro.getAltitudMsnm() != null ? registro.getAltitudMsnm() : 0.0;
            }

            // Cálculo de la nueva posición del dron en X e Y
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
            double nuevaAlt;

            // Obtener altitudes de inicio y fin de la ruta
            double altitudInicio = mision.getRuta().getStartPoint().getCoordinate().getZ();
            double altitudFin = mision.getRuta().getEndPoint().getCoordinate().getZ();

            // Manejar casos donde Z sea NaN
            if (Double.isNaN(altitudInicio)) altitudInicio = 0.0;
            if (Double.isNaN(altitudFin)) altitudFin = 0.0;

            // Calcular la distancia total de la ruta para normalizar
            double dxTotal = mision.getRuta().getEndPoint().getX() - mision.getRuta().getStartPoint().getX();
            double dyTotal = mision.getRuta().getEndPoint().getY() - mision.getRuta().getStartPoint().getY();
            double distanciaTotal = Math.sqrt(
                    Math.pow(dxTotal * metrosPorGradoLon, 2) +
                    Math.pow(dyTotal * metrosPorGradoLat, 2)
            );

            // Normalizar x entre 0 y 1 (progreso del vuelo)
            double progreso = distanciaTotal > 0 ? 1.0 - (distanciaRestante / distanciaTotal) : 1.0;
            progreso = Math.max(0.0, Math.min(1.0, progreso)); // Clamp entre 0 y 1

            // Interpolación lineal entre altitud inicio y fin, con una curva parabólica para simular ascenso/descenso
            // f(x) = altInicio + (altFin - altInicio) * x + 4 * altMax * x * (1 - x)
            // donde altMax es la altitud máxima adicional en el punto medio del vuelo
            double altitudMaxAdicional = 100.0; // Metros adicionales en el punto más alto
            nuevaAlt = altitudInicio + (altitudFin - altitudInicio) * progreso + 4 * altitudMaxAdicional * progreso * (1 - progreso);

            // Asegurar que nuevaAlt esté dentro de límites razonables
            if (nuevaAlt < 0) {
                nuevaAlt = 0;
            }
            if (nuevaAlt > 800) {
                nuevaAlt = 800;
            }

            // Verificar que el dron no se pasó de su destino en X e Y
            if (distanciaAvance >= distanciaRestante) {
                // Llegó o se pasó
                nuevaLat = destino.getY();
                nuevaLon = destino.getX();
                mision.setEstado("Completada");
                dron.setEstado("Disponible");
                dronRepository.update(dron);
                misionRepository.update(mision);
            } else {
                double modulo = Math.sqrt(dx*dx + dy*dy);
                double dirX = dx / modulo;
                double dirY = dy / modulo;
                double deltaLat = (dirY * distanciaAvance) / metrosPorGradoLat;
                double deltaLon = (dirX * distanciaAvance) / metrosPorGradoLon;
                nuevaLat = lat + deltaLat;
                nuevaLon = lon + deltaLon;
            }
            // Pasar las coordenadas a Point 3D y agregar tiempo
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coord3d = new Coordinate(nuevaLon, nuevaLat, nuevaAlt);
            Point nuevasCoordenadas = geometryFactory.createPoint(coord3d);
            nuevasCoordenadas.setSRID(4326); // Importante para PostGIS
            nuevoRegistro.setCoordenadas(nuevasCoordenadas);
            nuevoRegistro.setAltitudMsnm(nuevaAlt);
            nuevoRegistro.setTimestamp(LocalDateTime.now());
            registroVueloRepository.save(nuevoRegistro);
            registrosActualizados.add(nuevoRegistro);
        }
        return registrosActualizados;
    }

    public List<VelocidadCalculadaDTO> obtenerVelocidades(Long idMision) {
        // 1. Llamamos al repositorio que ya funciona
        List<VelocidadCalculadaDTO> reporte = registroVueloRepository.obtenerVelocidadesCalculadas(idMision);

        // 2. RETORNAMOS la lista al controlador (esto es lo que faltaba)
        return reporte;
    }


//    //Registro por id de dron
//    public List<RegistroVuelo> getRegistroVuelo(long id) {
//        //  obtener mision con id de dron
//        long idMision = misionRepository.findByDronId(id).getId().orElseThrow();
//        return registroVueloRepository.findByMisionId(idMision);
//    }
}