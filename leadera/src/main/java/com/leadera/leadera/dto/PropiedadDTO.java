package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.enums.TipoVivienda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista de Propiedad para respuestas HTTP. Mantiene los nombres de campo que ya
 * consume el frontend (tipoVivienda, observaciones, etc.) para no romper el contrato,
 * y agrega leadId para enlazar al lead sin exponer la entidad.
 */
public record PropiedadDTO(
        long id,
        String direccion,
        BigDecimal precio,
        Integer cantidadAmbientes,
        Integer metrosTotales,
        Integer metrosCubiertos,
        TipoVivienda tipoVivienda,
        String zona,
        String observaciones,
        String linkPortal,
        LocalDateTime fechaPublicacion,
        long diasEnMercado,
        EstadoPropiedad estado,
        Long leadId
) {
}
