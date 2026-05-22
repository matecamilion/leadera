package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompradorPotencialDTO {
    private Long leadId;
    private String leadNombre;
    private String leadTelefono;
    private EstadoLead leadEstado;
    private Long operacionId;
    private String busquedaZona;
    private String busquedaTipoVivienda;
    private BigDecimal busquedaPrecioMin;
    private BigDecimal busquedaPrecioMax;
    private Integer busquedaAmbientes;
    private Integer busquedaMetros;
}
