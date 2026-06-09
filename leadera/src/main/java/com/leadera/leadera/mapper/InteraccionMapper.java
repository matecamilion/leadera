package com.leadera.leadera.mapper;

import com.leadera.leadera.dto.InteraccionDTO;
import com.leadera.leadera.entity.Interaccion;

public class InteraccionMapper {

    private InteraccionMapper() {
    }

    public static InteraccionDTO toDTO(Interaccion interaccion) {
        if (interaccion == null) return null;
        return new InteraccionDTO(
                interaccion.getId(),
                interaccion.getTipoInteraccion(),
                interaccion.getDetalle(),
                interaccion.getFechaInteraccion()
        );
    }
}
