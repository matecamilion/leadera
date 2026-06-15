package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.TipoOperacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionDTO(
        Long id,
        String titulo,
        TipoOperacion tipoOperacion,
        EstadoOperacion estadoOperacion,
        String descripcion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        LocalDateTime fechaProximoSeguimiento,
        PropiedadDTO propiedad,   // null para operaciones de COMPRA
        BusquedaDTO busqueda,     // null para operaciones de VENTA
        Long leadId,
        BigDecimal montoOperacion
) {}
