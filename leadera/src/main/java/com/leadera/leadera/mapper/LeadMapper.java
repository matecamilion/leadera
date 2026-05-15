package com.leadera.leadera.mapper;

import com.leadera.leadera.dto.LeadRequestDTO;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.entity.Lead;

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

    public static Lead toEntity(LeadRequestDTO dto) {
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
}
