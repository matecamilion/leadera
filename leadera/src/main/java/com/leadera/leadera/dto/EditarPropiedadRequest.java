package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoVivienda;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditarPropiedadRequest {

    private String direccion;
    private BigDecimal precio;
    private Integer cantidadAmbientes;
    private Integer metrosTotales;
    private Integer metrosCubiertos;
    private TipoVivienda tipoVivienda;
    private String zona;
    private String observaciones;
}
