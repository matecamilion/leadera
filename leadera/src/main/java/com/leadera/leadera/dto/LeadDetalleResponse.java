package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDetalleResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private EstadoLead estado;
    private LocalDateTime fechaEntrada;
    private LocalDateTime ultimoContacto;
    private LocalDateTime fechaProximoSeguimiento;
    private String origen;
    private String descripcionInicial;
    private List<Interaccion> interacciones;
    private List<Propiedad> propiedades;
}
