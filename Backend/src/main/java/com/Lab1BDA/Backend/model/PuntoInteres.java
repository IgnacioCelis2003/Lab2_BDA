package com.Lab1BDA.Backend.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTWriter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntoInteres {
    private Long poi_id;
    private String nombre;
    private String descripcion;

    @JsonIgnore
    private Point ubicacion;
    @JsonProperty("ubicacionWKT")
    public String getUbicacionWKT() {
        if (this.ubicacion == null) return null;
        return new WKTWriter(3).write(this.ubicacion);
    }
}