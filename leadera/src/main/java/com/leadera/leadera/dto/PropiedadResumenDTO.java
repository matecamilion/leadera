package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.enums.TipoVivienda;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropiedadResumenDTO {

    private Long id;
    private String titulo;
    private TipoVivienda tipo;
    private EstadoPropiedad estado;
    private BigDecimal precio;
    private Long leadId;
    private String leadNombre;

    public static PropiedadResumenDTO fromEntity(Propiedad propiedad) {
        PropiedadResumenDTO dto = new PropiedadResumenDTO();
        dto.setId(propiedad.getId());
        dto.setTitulo(propiedad.getDireccion());
        dto.setTipo(propiedad.getTipoVivienda());
        dto.setEstado(propiedad.getEstado());
        dto.setPrecio(propiedad.getPrecio());

        Lead lead = propiedad.getLead();
        if (lead != null) {
            dto.setLeadId(lead.getId());
            dto.setLeadNombre(armarNombreLead(lead));
        }
        return dto;
    }

    private static String armarNombreLead(Lead lead) {
        String nombre = lead.getNombre() != null ? lead.getNombre() : "";
        String apellido = lead.getApellido() != null ? lead.getApellido() : "";
        String nombreCompleto = (nombre + " " + apellido).trim();
        return nombreCompleto.isEmpty() ? "Sin nombre" : nombreCompleto;
    }
}
