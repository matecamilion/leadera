package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadResponseDTO {
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
}
