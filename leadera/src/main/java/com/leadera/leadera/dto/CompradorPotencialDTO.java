package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Matching compartido a nivel inmobiliaria: este DTO NUNCA incluye teléfono
// ni email del lead. Cuando el lead es de otro agente del equipo (esMio=false)
// la idea es "hablá con [agente], tiene un comprador para tu propiedad";
// el dueño del lead accede al detalle completo por su propia cartera.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompradorPotencialDTO {
    private Long leadId;
    private String nombreLead;
    private String apellidoLead;
    private EstadoLead estadoLead;
    private Long operacionId;
    private String busquedaZona;
    private String busquedaTipoVivienda;
    private BigDecimal busquedaPrecioMin;
    private BigDecimal busquedaPrecioMax;
    private Integer busquedaAmbientes;
    private Integer busquedaMetros;
    private Long agenteId;
    private String nombreAgente;
    private String apellidoAgente;
    private boolean esMio;
}
