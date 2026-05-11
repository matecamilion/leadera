package com.leadera.leadera.service;

import com.leadera.leadera.model.EstadoPropiedad;
import com.leadera.leadera.model.EventoPropiedad;
import com.leadera.leadera.model.Propiedad;
import com.leadera.leadera.model.Lead;
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
                .orElseThrow(() -> new RuntimeException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new RuntimeException("No tenés permiso para modificar este lead.");
        }

        propiedad.setLead(lead);
        return propiedadRepository.save(propiedad);
    }

    public List<Propiedad> obtenerPropiedadesDeLead(Long leadId) {
        return propiedadRepository.findByLeadId(leadId);
    }

    public EventoPropiedad registrarEvento(Long propiedadId, EventoPropiedad evento) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));

        long numeroSiguiente = eventoRepository.countByPropiedadId(propiedadId) + 1;
        evento.setFecha(LocalDateTime.now());
        evento.setPropiedad(propiedad);

        return eventoRepository.save(evento);
    }

    public List<EventoPropiedad> obtenerEventos(Long propiedadId) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        return propiedad.getEventos();
    }

    public Propiedad actualizarEstado(Long id, String estado) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        propiedad.setEstado(EstadoPropiedad.valueOf(estado));
        return propiedadRepository.save(propiedad);
    }

    public Propiedad obtenerPorId(Long id) {
        return propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
    }

}
