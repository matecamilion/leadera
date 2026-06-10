package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body de POST /leads/{leadId}/operaciones/{operacionId}/eventos. Sin id,
 * fecha ni operacion: los asigna el backend.
 */
public record CrearEventoRequest(
        @NotNull TipoEvento tipo,
        @NotBlank String detalle
) {}
