package com.leadera.leadera.model;

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
    private String ultimaInteraccion; // detalle de la última
}