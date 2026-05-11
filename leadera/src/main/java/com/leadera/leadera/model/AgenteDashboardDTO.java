package com.leadera.leadera.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AgenteDashboardDTO {
    long activos;
    long calientes;
    long tibios;
    long frios;
    long ganadosMes;
    long nuevosDelMes;
    long perdidos;
    long interacciones7d;
    double tasaConversion;
    double tiempoRespuesta;
}
