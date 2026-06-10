package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoVivienda;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Body de POST /propiedades/lead/{leadId}. Sin id, lead, estado ni
 * fechaPublicacion: esos los controla el backend para evitar que un
 * cliente pise propiedades ajenas vía merge de JPA.
 */
public record CrearPropiedadRequest(
        @NotBlank String direccion,
        BigDecimal precio,
        Integer cantidadAmbientes,
        Integer metrosTotales,
        Integer metrosCubiertos,
        TipoVivienda tipoVivienda,
        String zona,
        String observaciones
) {}
