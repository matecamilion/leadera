package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoEvento;

import java.time.LocalDateTime;

public record EventoOperacionDTO(
        Long id,
        TipoEvento tipo,
        String detalle,
        LocalDateTime fecha
) {
}
