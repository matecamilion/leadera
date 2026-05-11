package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeadResumenDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private EstadoLead estado;
    private java.time.LocalDateTime fechaEntrada;
    private java.time.LocalDateTime ultimoContacto;
    private java.time.LocalDateTime fechaProximoSeguimiento;
    private long operacionesVenta;
    private long operacionesCompra;
    private long cantidadInteracciones;
    private String ultimaInteraccion;
}
