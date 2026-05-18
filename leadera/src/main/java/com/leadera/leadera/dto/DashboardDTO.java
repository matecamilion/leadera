package com.leadera.leadera.dto;

import java.util.List;

public record DashboardDTO(
        String periodo,
        int diasPeriodo,
        SnapshotDTO snapshot,
        KpisDTO kpis,
        EvolucionDTO evolucion,
        List<OrigenDTO> origenes
) {
    public record SnapshotDTO(
            long activos,
            long calientes,
            long tibios,
            long frios,
            long ganadosMes,
            int metaMensual,
            int diasRestantesMes,
            FunnelDTO funnel
    ) {}

    public record FunnelDTO(
            long contactados,
            long calificados,
            long visita,
            long oferta,
            long cerrado
    ) {}

    public record KpisDTO(
            KpiDTO leadsActivos,
            KpiDTO tasaConversion,
            KpiDTO tiempoRespuesta,
            KpiDTO ganados
    ) {}

    public record KpiDTO(
            double actual,
            double anterior,
            double deltaPct,
            String direccion,
            List<Double> sparkline
    ) {}

    public record EvolucionDTO(
            List<String> fechas,
            List<Long> nuevos,
            List<Long> ganados,
            List<Long> perdidos
    ) {}

    public record OrigenDTO(
            String nombre,
            long cantidad,
            double porcentaje
    ) {}
}
