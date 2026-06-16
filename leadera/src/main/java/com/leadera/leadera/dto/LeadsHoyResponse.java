package com.leadera.leadera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadsHoyResponse {

    private List<LeadHoyDTO> nuevosSinContacto;
    private List<LeadHoyDTO> prioritarios;
    private List<LeadHoyDTO> seguimientosDeHoy;

    private List<LeadHoyDTO> contactadosHoy;

    private int totalTareasDelDia;
    private int tareasCompletadasDelDia;

    private int totalPrioritarios;
    private int totalNuevos;
    private int totalSeguimientos;

}
