package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadHoyDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private EstadoLead estado;
    private LocalDateTime ultimoContacto;
    private String ultimaInteraccion;
}
