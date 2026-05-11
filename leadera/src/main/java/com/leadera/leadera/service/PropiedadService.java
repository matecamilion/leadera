package com.leadera.leadera.service;

import com.leadera.leadera.entity.EventoPropiedad;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.EventoPropiedadRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropiedadService {
    private final PropiedadRepository propiedadRepository;
    private final EventoPropiedadRepository eventoRepository;
    private final LeadRepository leadRepository;


    public PropiedadService(PropiedadRepository propiedadRepository,
                            EventoPropiedadRepository eventoRepository,
                            LeadRepository leadRepository) {
        this.propiedadRepository = propiedadRepository;
        this.eventoRepository = eventoRepository;
        this.leadRepository = leadRepository;
    }

    public Propiedad agregarPropiedad(Long leadId, Propiedad propiedad, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

        propiedad.setLead(lead);
        return propiedadRepository.save(propiedad);
    }

    public List<Propiedad> obtenerPropiedadesDeLead(Long leadId) {
        return propiedadRepository.findByLeadId(leadId);
    }

    public EventoPropiedad registrarEvento(Long propiedadId, EventoPropiedad evento) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        evento.setFecha(LocalDateTime.now());
        evento.setPropiedad(propiedad);

        return eventoRepository.save(evento);
    }

    public List<EventoPropiedad> obtenerEventos(Long propiedadId) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
        return propiedad.getEventos();
    }

    public Propiedad actualizarEstado(Long id, String estado) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
        try {
            propiedad.setEstado(EstadoPropiedad.valueOf(estado));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado de propiedad inválido: " + estado);
        }
        return propiedadRepository.save(propiedad);
    }

    public Propiedad obtenerPorId(Long id) {
        return propiedadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
    }

}
