package com.leadera.leadera.service;


import com.leadera.leadera.dto.CrearInteraccionRequest;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.TipoInteraccion;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class InteraccionService {
    private final InteraccionRepository interaccionRepository;
    private final LeadRepository leadRepository;
    private final ZoneId zonaHoraria;

    public InteraccionService(InteraccionRepository interaccionRepository,
                              LeadRepository leadRepository,
                              ZoneId zonaHoraria) {
        this.interaccionRepository = interaccionRepository;
        this.leadRepository = leadRepository;
        this.zonaHoraria = zonaHoraria;
    }

    public Interaccion crearInteraccion(Long leadId, CrearInteraccionRequest request, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para registrar interacciones en este lead.");
        }

        Interaccion interaccion = new Interaccion();
        interaccion.setTipoInteraccion(
                request.getTipoInteraccion() != null ? request.getTipoInteraccion() : TipoInteraccion.LLAMADA
        );
        interaccion.setDetalle(request.getDetalle());
        interaccion.setFechaInteraccion(
                request.getFechaInteraccion() != null ? request.getFechaInteraccion() : LocalDateTime.now(zonaHoraria)
        );
        interaccion.setLead(lead);

        lead.setUltimoContacto(interaccion.getFechaInteraccion());

        if (request.getProximoContacto() != null) {
            lead.setFechaProximoSeguimiento(request.getProximoContacto());
        } else {
            lead.setFechaProximoSeguimiento(LocalDateTime.now(zonaHoraria).plusDays(3));
        }

        leadRepository.save(lead);
        return interaccionRepository.save(interaccion);
    }


}
