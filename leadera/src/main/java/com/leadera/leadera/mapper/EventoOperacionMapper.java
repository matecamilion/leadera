package com.leadera.leadera.mapper;

import com.leadera.leadera.dto.EventoOperacionDTO;
import com.leadera.leadera.entity.EventoOperacion;

public class EventoOperacionMapper {

    private EventoOperacionMapper() {
    }

    public static EventoOperacionDTO toDTO(EventoOperacion evento) {
        if (evento == null) return null;
        return new EventoOperacionDTO(
                evento.getId(),
                evento.getTipo(),
                evento.getDetalle(),
                evento.getFecha()
        );
    }
}
