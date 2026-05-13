package com.leadera.leadera.dto;

import com.leadera.leadera.entity.EventoOperacion;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.enums.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventoOperacionResumenDTO {

    private Long id;
    private TipoEvento tipo;
    private LocalDateTime fecha;
    private String detalle;
    private Long operacionId;
    private String operacionTitulo;
    private Long leadId;

    public static EventoOperacionResumenDTO fromEntity(EventoOperacion evento) {
        EventoOperacionResumenDTO dto = new EventoOperacionResumenDTO();
        dto.setId(evento.getId());
        dto.setTipo(evento.getTipo());
        dto.setFecha(evento.getFecha());
        dto.setDetalle(evento.getDetalle());

        Operacion op = evento.getOperacion();
        if (op != null) {
            dto.setOperacionId(op.getId());
            dto.setOperacionTitulo(op.getTitulo());
            if (op.getLead() != null) {
                dto.setLeadId(op.getLead().getId());
            }
        }
        return dto;
    }
}
