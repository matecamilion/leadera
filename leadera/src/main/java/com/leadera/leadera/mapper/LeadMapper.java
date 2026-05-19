package com.leadera.leadera.mapper;

import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadHoyDTO;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;

import java.util.List;

public class LeadMapper {

    private LeadMapper() {
    }

    public static LeadResponseDTO toDTO(Lead lead) {
        if (lead == null) return null;
        return new LeadResponseDTO(
                lead.getId(),
                lead.getNombre(),
                lead.getApellido(),
                lead.getTelefono(),
                lead.getEmail(),
                lead.getEstado(),
                lead.getFechaEntrada(),
                lead.getUltimoContacto(),
                lead.getFechaProximoSeguimiento(),
                lead.getOrigen(),
                lead.getDescripcionInicial()
        );
    }

    public static LeadHoyDTO toHoyDTO(Lead lead) {
        if (lead == null) return null;
        String ultima = null;
        List<Interaccion> interacciones = lead.getInteracciones();
        if (interacciones != null && !interacciones.isEmpty()) {
            ultima = interacciones.get(interacciones.size() - 1).getDetalle();
        }
        return new LeadHoyDTO(
                lead.getId(),
                lead.getNombre(),
                lead.getApellido(),
                lead.getEstado(),
                lead.getUltimoContacto(),
                ultima
        );
    }

    public static Lead toEntity(CrearLeadRequest dto) {
        if (dto == null) return null;
        Lead lead = new Lead();
        lead.setNombre(dto.getNombre());
        lead.setApellido(dto.getApellido());
        lead.setTelefono(dto.getTelefono());
        lead.setEmail(dto.getEmail());
        lead.setEstado(dto.getEstado());
        lead.setFechaProximoSeguimiento(dto.getFechaProximoSeguimiento());
        lead.setOrigen(dto.getOrigen());
        lead.setDescripcionInicial(dto.getDescripcionInicial());
        return lead;
    }

    public static LeadDetalleResponse toDetalleResponse(Lead lead) {
        if (lead == null) return null;
        return new LeadDetalleResponse(
                lead.getId(),
                lead.getNombre(),
                lead.getApellido(),
                lead.getTelefono(),
                lead.getEmail(),
                lead.getEstado(),
                lead.getFechaEntrada(),
                lead.getUltimoContacto(),
                lead.getFechaProximoSeguimiento(),
                lead.getOrigen(),
                lead.getDescripcionInicial(),
                lead.getInteracciones(),
                lead.getPropiedades()
        );
    }
}
