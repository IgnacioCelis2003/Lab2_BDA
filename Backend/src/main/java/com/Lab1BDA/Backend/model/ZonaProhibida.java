package com.Lab1BDA.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonaProhibida {
    private Long id;
    private String nombre;
    private Polygon area; // JTS Polygon mapeado a GEOMETRY(POLYGON, 4326)
}