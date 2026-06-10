package com.leadera.leadera.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Estadísticas agregadas de la inmobiliaria + desglose por agente activo.
@Getter
@Setter
@AllArgsConstructor
public class EquipoStatsDTO {

    private AgenteDashboardDTO totales;
    private List<AgenteStatsDTO> porAgente;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class AgenteStatsDTO {
        private Long agenteId;
        private String nombre;
        private String apellido;
        private AgenteDashboardDTO stats;
    }
}
