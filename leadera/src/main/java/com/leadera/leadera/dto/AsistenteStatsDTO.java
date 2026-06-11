package com.leadera.leadera.dto;

// Fila de la vista "Mi equipo" del AGENTE: un asistente con sus métricas del día.
// leadsActivos son los del SUPERVISOR (el asistente no tiene leads propios:
// trabaja sobre los de su agente).
public record AsistenteStatsDTO(
        Long id,
        String nombre,
        String apellido,
        String email,
        boolean activo,
        long leadsActivos,
        long tareasCompletadasHoy
) {
}
