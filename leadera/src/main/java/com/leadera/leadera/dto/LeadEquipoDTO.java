package com.leadera.leadera.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// Fila del listado read-only "Leads del equipo" del dueño:
// el resumen habitual del lead + el agente al que pertenece.
@Getter
@Setter
@AllArgsConstructor
public class LeadEquipoDTO {
    private LeadResumenDTO lead;
    private Long agenteId;
    private String agenteNombre;
    private String agenteApellido;
}
