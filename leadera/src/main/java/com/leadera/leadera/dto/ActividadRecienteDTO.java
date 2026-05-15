package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.enums.TipoInteraccion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActividadRecienteDTO {
    private TipoInteraccion tipoInteraccion;
    private String detalle;
    private LocalDateTime fechaInteraccion;
    private String leadNombre;
    private String leadApellido;
    private Long leadId;

    public static ActividadRecienteDTO fromEntity(Interaccion i) {
        return new ActividadRecienteDTO(
                i.getTipoInteraccion(),
                i.getDetalle(),
                i.getFechaInteraccion(),
                i.getLead() != null ? i.getLead().getNombre() : null,
                i.getLead() != null ? i.getLead().getApellido() : null,
                i.getLead() != null ? i.getLead().getId() : null
        );
    }
}
