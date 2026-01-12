package com.Lab1BDA.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString; // ¡Usamos la librería JTS que importamos!
import org.locationtech.jts.io.WKTWriter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mision {
    private Long idMision;
    private Long idDronAsignado;
    private Long idTipoMision;
    private Long idOperadorCreador;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicioPlanificada;
    private LocalDateTime fechaFinPlanificada;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFinReal;
    private String estado; // El ENUM 'estado_mision'
    @JsonIgnore // Evita la serialización recursiva que causa el "envelope" infinito
    private LineString ruta; // PostGIS LineString se mapea a JTS LineString

    // Exponer la ruta como WKT (string) en el JSON de salida para evitar problemas
    // de serialización con la librería JTS.
    @JsonProperty("rutaWKT")
    public String getRutaWKT() {
        if (this.ruta == null) return null;
        return new WKTWriter().write(this.ruta);
    }
}