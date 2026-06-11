package com.leadera.leadera.service;


import com.leadera.leadera.dto.CrearInteraccionRequest;
import com.leadera.leadera.entity.Agente;
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
    private final AgenteContextResolver agenteContextResolver;
    private final ZoneId zonaHoraria;

    public InteraccionService(InteraccionRepository interaccionRepository,
                              LeadRepository leadRepository,
                              AgenteContextResolver agenteContextResolver,
                              ZoneId zonaHoraria) {
        this.interaccionRepository = interaccionRepository;
        this.leadRepository = leadRepository;
        this.agenteContextResolver = agenteContextResolver;
        this.zonaHoraria = zonaHoraria;
    }

    public Interaccion crearInteraccion(Long leadId, CrearInteraccionRequest request, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        // El actor es quien registra (puede ser un asistente); el propietario es
        // el dueño efectivo del lead (su supervisor, o él mismo). Solo se permite
        // operar el lead si pertenece al propietario efectivo del actor.
        Agente actor = agenteContextResolver.resolverActor(emailAgente);
        Agente propietario = agenteContextResolver.resolverPropietario(actor);

        if (!lead.getAgente().getId().equals(propietario.getId())) {
            throw new UnauthorizedActionException("No tenés permiso para registrar interacciones en este lead.");
        }

        Interaccion interaccion = new Interaccion();
        // autor = el agente REAL que la registró (el asistente, no el supervisor).
        interaccion.setAutor(actor);
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
