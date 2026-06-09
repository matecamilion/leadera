package com.leadera.leadera.mapper;

import com.leadera.leadera.dto.PropiedadDTO;
import com.leadera.leadera.entity.Propiedad;

public class PropiedadMapper {

    private PropiedadMapper() {
    }

    public static PropiedadDTO toDTO(Propiedad propiedad) {
        if (propiedad == null) return null;
        Long leadId = propiedad.getLead() != null ? propiedad.getLead().getId() : null;
        return new PropiedadDTO(
                propiedad.getId(),
                propiedad.getDireccion(),
                propiedad.getPrecio(),
                propiedad.getCantidadAmbientes(),
                propiedad.getMetrosTotales(),
                propiedad.getMetrosCubiertos(),
                propiedad.getTipoVivienda(),
                propiedad.getZona(),
                propiedad.getObservaciones(),
                propiedad.getFechaPublicacion(),
                propiedad.getDiasEnMercado(),
                propiedad.getEstado(),
                leadId
        );
    }
}
