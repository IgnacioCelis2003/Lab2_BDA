package com.Lab1BDA.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point; // ¡Usamos la librería JTS que importamos!
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVuelo {
    private Long idRegistroVuelo;
    private Long idMision;
    private LocalDateTime timestamp;
    private Point coordenadas;
    private Double altitudMsnm;
    private Double velocidadKmh;
    private Double nivelBateriaPorcentaje;
}