package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.enums.TipoVivienda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CrearOperacionRequest(
        @NotBlank String titulo,
        @NotNull TipoOperacion tipoOperacion,
        String descripcion,
        PropiedadRef propiedad,
        BusquedaData busqueda
) {
    // Referencia a propiedad existente por id (solo para VENTA).
    // Jackson ignora cualquier otro campo que venga en el JSON.
    public record PropiedadRef(Long id) {}

    // Datos de búsqueda para operaciones de COMPRA.
    // No tiene campo id: imposibilita pisar una Busqueda existente.
    public record BusquedaData(
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
}
