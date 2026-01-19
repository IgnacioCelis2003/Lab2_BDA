package com.Lab1BDA.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point; // ¡Usamos la librería JTS que importamos!
import org.locationtech.jts.io.WKTWriter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVuelo {
    private Long idRegistroVuelo;
    private Long idMision;
    private LocalDateTime timestamp;
    @JsonIgnore
    private Point coordenadas;
    private Double altitudMsnm;
    private Double velocidadKmh;
    private Double nivelBateriaPorcentaje;

    @JsonProperty("coordenadasWKT")
    public String getcoordenadasWKT() {
        if (this.coordenadas == null) return null;
        return new WKTWriter(3).write(this.coordenadas);
    }

    public Double getAltitudMsnm() {
        return altitudMsnm;
    }
}