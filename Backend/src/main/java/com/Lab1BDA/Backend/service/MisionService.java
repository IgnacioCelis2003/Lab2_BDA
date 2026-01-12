package com.Lab1BDA.Backend.service;

import com.Lab1BDA.Backend.dto.*;
import com.Lab1BDA.Backend.exception.ResourceNotFoundException;
import com.Lab1BDA.Backend.model.*;
import com.Lab1BDA.Backend.repository.*;
import org.locationtech.jts.geom.Geometry; // Importación necesaria
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MisionService {

    @Autowired
    private MisionRepository misionRepository;

    @Autowired
    private TipoMisionRepository tipoMisionRepository;

    @Autowired
    private DronRepository dronRepository;

    @Autowired
    private UserRepository userRepository; // Necesario para obtener el creador

    // Este WKTReader convierte el String del DTO a un objeto LineString
    private final WKTReader wktReader = new WKTReader();
    @Autowired
    private ModeloDronRepository modeloDronRepository;
    @Autowired
    private RegistroVueloRepository registroVueloRepository;
    @Autowired
    private ResumenMisionesService resumenMisionesService;

    public List<Mision> getTodasLasMisiones() {
        return misionRepository.findAll();
    }

    public Mision getMisionPorId(Long id) {
        return misionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Misión no encontrada con id: " + id));
    }

    public Mision crearMision(MisionRequestDTO dto, String emailCreador) {
        // 1. Buscar al usuario creador por su email
        Usuario creador = userRepository.findByEmail(emailCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado: " + emailCreador));

        Mision mision = new Mision();
        mision.setIdTipoMision(dto.idTipoMision());
        mision.setIdDronAsignado(dto.idDronAsignado());
        mision.setFechaInicioPlanificada(dto.fechaInicioPlanificada());
        mision.setFechaFinPlanificada(dto.fechaFinPlanificada());

        // 2. Establecer campos de negocio
        mision.setIdOperadorCreador(creador.getIdUsuario());
        mision.setEstado(dto.estado() != null ? dto.estado() : "Pendiente"); // Default

        // 3. Convertir WKT (String) a LineString (JTS)
        mision.setRuta(convertWKTToLineString(dto.rutaWKT()));

        // 4. Pasa la Mision (con el LineString) al repositorio
        return misionRepository.save(mision);
    }

    public Mision actualizarMision(Long id, MisionRequestDTO dto) {
        // 1. Verificar que la misión existe
        Mision misionExistente = getMisionPorId(id);

        // 2. Actualizar campos
        misionExistente.setIdTipoMision(dto.idTipoMision());
        misionExistente.setIdDronAsignado(dto.idDronAsignado());
        misionExistente.setFechaInicioPlanificada(dto.fechaInicioPlanificada());
        misionExistente.setFechaFinPlanificada(dto.fechaFinPlanificada());
        misionExistente.setEstado(dto.estado());

        // 3. Convertir WKT (String) a LineString (JTS)
        misionExistente.setRuta(convertWKTToLineString(dto.rutaWKT()));

        // 4. Pasa la Mision (con el LineString) al repositorio
        return misionRepository.update(misionExistente);
    }

    public void iniciarMision(long id){
        Mision mision = misionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Misión no encontrada con id: " + id));
        mision.setEstado("En Progreso");
        mision.setFechaInicioReal(LocalDateTime.now());

        Dron dron = dronRepository.findById(mision.getIdDronAsignado())
                .orElseThrow();
        ModeloDron modelo = modeloDronRepository.findById(dron.getIdModelo()).orElseThrow();
        dron.setEstado("En Vuelo");

        RegistroVuelo registroVuelo = new RegistroVuelo();
        registroVuelo.setIdMision(id);
        registroVuelo.setTimestamp(LocalDateTime.now());
        registroVuelo.setCoordenadas(mision.getRuta().getStartPoint());
        registroVuelo.setNivelBateriaPorcentaje(100.0);
        registroVuelo.setVelocidadKmh(modelo.getVelocidadPromedioKmh());

        dronRepository.update(dron);
        misionRepository.update(mision);
        registroVueloRepository.save(registroVuelo);
    }

    /**
     * Completa una misión (cambia su estado a "Completada" y registra la fecha fin real).
     * También refresca la vista materializada de misiones completadas.
     * @param id ID de la misión a completar
     */
    public void completarMision(Long id) {
        Mision mision = misionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Misión no encontrada con id: " + id));

        mision.setEstado("Completada");
        mision.setFechaFinReal(LocalDateTime.now());

        // Actualizar dron a "Disponible"
        if (mision.getIdDronAsignado() != null) {
            Dron dron = dronRepository.findById(mision.getIdDronAsignado())
                    .orElseThrow(() -> new ResourceNotFoundException("Dron no encontrado"));
            dron.setEstado("Disponible");
            dronRepository.update(dron);
        }

        misionRepository.update(mision);

        // Refrescar la vista materializada de misiones completadas
        resumenMisionesService.refreshResumenMisiones();
    }

    public void eliminarMision(Long id) {
        getMisionPorId(id); // Verifica que existe
        misionRepository.deleteById(id);
    }

    /**
     * Helper para convertir un String WKT a un objeto LineString de JTS.
     */
    private LineString convertWKTToLineString(String wkt) {
        if (wkt == null || wkt.isEmpty()) {
            return null;
        }
        try {
            Geometry geom = wktReader.read(wkt);
            if (geom instanceof LineString) {
                return (LineString) geom;
            }
            throw new IllegalArgumentException("El WKT proporcionado no es un LINESTRING");
        } catch (ParseException e) {
            // Esto debería devolver un 400 Bad Request
            throw new IllegalArgumentException("Formato WKT de la ruta es inválido: " + wkt, e);
        }
    }

    /**
     * Lógica de negocio para asignar una misión a un dron.
     * Llama al procedimiento almacenado en la BD.
     * * @param idMision El ID de la misión.
     * @param idDron El ID del dron.
     */
    // Pero como el SP ya es atómico, solo delegamos la llamada.
    public void asignarMisionADron(Long idMision, Long idDron) {
        // La lógica de validación (si el dron está 'Disponible', etc.)
        // ya está encapsulada dentro del Procedimiento Almacenado,
        // cumpliendo el requisito del enunciado[cite: 60].

        // Simplemente llamamos al repositorio.
        misionRepository.asignarMisionADron(idMision, idDron);
    }

    /**
     * Lógica de negocio para asignar una lista de misiones a varios drones.
     * Llama al procedimiento almacenado en la BD.
     * @param idMisiones Lista con los ID de las misiones a optimizar
     * @return DTO con la ruta óptima de los drones
     */
    public RutaOptimaResponseDTO generarRutaOptimaMultidron(List<Long> idMisiones) {
        // 1. Obtener datos de la BD
        List<DronSpecsDTO> drones = dronRepository.findDronesDisponiblesConSpecs();

        if (drones.isEmpty()) {
            return new RutaOptimaResponseDTO(new ArrayList<>(), idMisiones, "Error: No hay drones disponibles en la flota.");
        }

        // Obtenemos las misiones "crudas"
        List<Mision> misionesRaw = misionRepository.findMisionesPorIds(idMisiones);

        // Filtramos para quedarnos solo con las que realmente se pueden asignar
        List<Mision> misiones = misionesRaw.stream()
                .filter(m -> m.getIdDronAsignado() == null)      // Que no tenga dron
                .filter(m -> "Pendiente".equals(m.getEstado()))  // Que esté Pendiente
                .toList();

        // Si después del filtro nos quedamos sin misiones, retornamos aviso
        if (misiones.isEmpty()) {
            return new RutaOptimaResponseDTO(new ArrayList<>(), new ArrayList<>(), "Ninguna misión válida (todas estaban asignadas o no pendientes)");
        }

        List<DistanciaMisionDTO> matrizDistancias = misionRepository.calcularMatrizDistancias(idMisiones);

        // 2. Estructuras para guardar el progreso global
        List<RutaAsignadaDTO> rutasFinales = new ArrayList<>();
        Set<Long> misionesAsignadasIds = new HashSet<>();

        // 3. Procesar cada dron uno por uno
        for (DronSpecsDTO dron : drones) {
            RutaAsignadaDTO ruta = planificarVueloDron(dron, misiones, matrizDistancias, misionesAsignadasIds);
            rutasFinales.add(ruta);
        }

        // 4. Identificar misiones que sobraron (Huérfanas)
        List<Long> huerfanas = misiones.stream()
                .map(Mision::getIdMision)
                .filter(id -> !misionesAsignadasIds.contains(id))
                .toList();

        return new RutaOptimaResponseDTO(rutasFinales, huerfanas, "Optimización finalizada.");
    }

    /**
     * Obtiene el reporte de desempeño por tipo de misión (Requisito #3).
     * @return Lista de DesempenoTipoMisionDTO
     */
    public List<DesempenoTipoMisionDTO> getReporteDesempenoTipoMision() {
        return misionRepository.findDesempenoPorTipoMision();
    }

    /**
     * Obtiene el reporte de patrones de consumo de batería (Requisito #4).
     * @return Lista de BateriaConsumoDTO
     */
    public List<BateriaConsumoDTO> getReporteConsumoBateria() {
        return misionRepository.findPatronConsumoBateria();
    }

    /**
     * Obtiene el reporte de desempeño mensual (Requisito #5).
     * @return Lista de DesempenoMensualDTO
     */
    public List<DesempenoMensualDTO> getReporteDesempenoMensual() {
        return misionRepository.findDesempenoMensual();
    }

    /**
     * Obtiene el reporte de resumen de misiones por tipo (Requisito #10).
     * @return Lista de ResumenMisionTipoDTO
     */
    public List<ResumenMisionTipoDTO> getReporteResumenMisiones() {
        return misionRepository.findResumenMisionesCompletadas();
    }

    // Métodos auxiliares
    /**
     * Intenta llenar la agenda de un solo dron hasta que se quede sin batería o carga.
     * @param dron DTO con las especificaciones de un dron
     * @param todasLasMisiones Lista de todas las misiones aún no asignadas
     * @param matriz Matriz de la distancia entre misiones
     * @param yaAsignadas ID de las misiones ya asignadas
     * @return DTO con la ruta asignada a un dron
     */
     private RutaAsignadaDTO planificarVueloDron(DronSpecsDTO dron, List<Mision> todasLasMisiones,
                                                List<DistanciaMisionDTO> matriz, Set<Long> yaAsignadas) {
        List<MisionOrdenadaDTO> pasos = new ArrayList<>();

        // Datos del Dron
        double bateria = dron.autonomiaMinutos();

        // Carga: Solo informativo, no lo usamos para restar.
        double capacidadInformativa = dron.capacidadCargaKg();

        double distTotal = 0.0;
        double tiempoTotal = 0.0;
        Long ubicacionActual = null; // null = Base

        boolean puedeSeguir = true;

        // Obtenemos la velocidad del dron
        double velocidadKmh = dron.velocidadKmh();

        // Se convierte a m/min
        final double VELOCIDAD_METROS_MIN = (velocidadKmh * 1000.0) / 60.0;

        while (puedeSeguir) {
            // Pasamos la velocidad convertida al buscador
            Mision candidata = buscarSiguienteMision(ubicacionActual, todasLasMisiones, matriz, yaAsignadas, bateria, VELOCIDAD_METROS_MIN);

            if (candidata != null) {
                yaAsignadas.add(candidata.getIdMision());
                ubicacionActual = candidata.getIdMision();

                double dist = obtenerDistancia(matriz, ubicacionActual, candidata.getIdMision());

                // Usamos la velocidad convertida para calcular el tiempo de viaje
                double costoTiempo = calcularCostoTiempo(candidata, dist, VELOCIDAD_METROS_MIN);

                bateria -= costoTiempo;
                distTotal += dist;
                tiempoTotal += costoTiempo;

                // Agregar paso
                String nombreTipo = tipoMisionRepository.findById(candidata.getIdTipoMision())
                        .map(t -> t.getNombreTipo()).orElse("Misión");

                pasos.add(new MisionOrdenadaDTO(pasos.size() + 1, candidata.getIdMision(), nombreTipo, candidata.getRutaWKT()));
            } else {
                puedeSeguir = false;
            }
        }

        return new RutaAsignadaDTO(dron.idDron(), dron.nombreModelo(), pasos, distTotal, tiempoTotal, bateria, capacidadInformativa);
    }

    /**
     * Busca la misión no asignada más cercana que cumpla con las restricciones de batería y carga.
     * @param origen misión de origen
     * @param misiones Lista de las misiones aún no asignadas
     * @param matriz Matriz de la distancia entre misiones
     * @param asignadas Set con el id de las misiones ya asignadas al dron
     * @param bateriaRestante cantidad de batería restante del dron
     * @param velocidadMetrosMin velocidad del dron en metros por minuto
     * @return Mision no asignada más cercana
     */
    private Mision buscarSiguienteMision(Long origen, List<Mision> misiones, List<DistanciaMisionDTO> matriz,
                                         Set<Long> asignadas, double bateriaRestante, double velocidadMetrosMin) {
        Mision mejor = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Mision m : misiones) {
            if (asignadas.contains(m.getIdMision())) continue;

            double dist = obtenerDistancia(matriz, origen, m.getIdMision());

            double costoTiempo = calcularCostoTiempo(m, dist, velocidadMetrosMin);

            if (costoTiempo <= bateriaRestante) {
                if (dist < menorDistancia) {
                    menorDistancia = dist;
                    mejor = m;
                }
            }
        }
        return mejor;
    }

    // Helpers Matemáticos
    /**
     * Obtiene la distancia entre dos misiones en metros
     * @param matriz Matriz de la distancia entre misiones
     * @param origen Misión de origen
     * @param destino Misión de destino
     * @return Distancia entre dos misiones
     */
    private double obtenerDistancia(List<DistanciaMisionDTO> matriz, Long origen, Long destino) {
        if (origen == null) return 0.0;

        return matriz.stream()
                .filter(d -> (d.idOrigen().equals(origen) && d.idDestino().equals(destino)) ||
                        (d.idOrigen().equals(destino) && d.idDestino().equals(origen)))
                .mapToDouble(DistanciaMisionDTO::distanciaMetro)
                .findFirst().orElse(100000.0);
    }

    /**
     * Busca la misión no asignada más cercana que cumpla con las restricciones de batería y carga.
     * @param m mision a la que se le calcula la duración
     * @return Duración de la misión
     */
    private double calcularDuracionMision(Mision m) {
        if (m.getFechaInicioPlanificada() != null && m.getFechaFinPlanificada() != null) {
            long min = Duration.between(m.getFechaInicioPlanificada(), m.getFechaFinPlanificada()).toMinutes();
            return Math.max(min, 10);
        }
        return 0.0;
    }

    /**
     * Calcula el costo total en minutos (Viaje + Ejecución).
     * @param m Misión a la que se le calcula
     * @param distancia distancia entre misiones
     * @param velocidadMetrosMin velocidad del dron
     * @return costo de la misión
     */
    private double calcularCostoTiempo(Mision m, double distancia, double velocidadMetrosMin) {
        double tiempoViaje = distancia / velocidadMetrosMin;
        return tiempoViaje + calcularDuracionMision(m);
    }

}