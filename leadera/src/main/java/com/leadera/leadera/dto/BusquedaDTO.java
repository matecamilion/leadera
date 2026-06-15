package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoVivienda;
import java.math.BigDecimal;

public record BusquedaDTO(
        Long id,
        BigDecimal precioMin,
        BigDecimal precioMax,
        Integer cantidadAmbientes,
        Integer metrosTotales,
        Integer metrosCubiertos,
        Integer metrosDescubiertos,
        TipoVivienda tipoVivienda,
        String zona,
        String observaciones
) {}
