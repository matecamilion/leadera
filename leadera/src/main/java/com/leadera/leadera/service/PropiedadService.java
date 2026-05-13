package com.leadera.leadera.service;

import com.leadera.leadera.dto.EditarPropiedadRequest;
import com.leadera.leadera.dto.EventoOperacionResumenDTO;
import com.leadera.leadera.dto.PropiedadResumenDTO;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.EventoOperacionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropiedadService {
    private final PropiedadRepository propiedadRepository;
    private final LeadRepository leadRepository;
    private final EventoOperacionRepository eventoOperacionRepository;

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public PropiedadService(PropiedadRepository propiedadRepository,
                            LeadRepository leadRepository,
                            EventoOperacionRepository eventoOperacionRepository) {
        this.propiedadRepository = propiedadRepository;
        this.leadRepository = leadRepository;
        this.eventoOperacionRepository = eventoOperacionRepository;
    }

    public Propiedad agregarPropiedad(Long leadId, Propiedad propiedad, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

        propiedad.setLead(lead);
        propiedad.setFechaPublicacion(LocalDateTime.now(ZONA_ARGENTINA));
        return propiedadRepository.save(propiedad);
    }

    public List<Propiedad> obtenerPropiedadesDeLead(Long leadId) {
        return propiedadRepository.findByLeadId(leadId);
    }

    public List<PropiedadResumenDTO> obtenerPropiedadesDelAgente(String emailAgente) {
        return propiedadRepository.findByAgenteEmail(emailAgente).stream()
                .map(PropiedadResumenDTO::fromEntity)
                .collect(Collectors.toList());
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

    public Propiedad editarPropiedad(Long propiedadId, EditarPropiedadRequest request, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para editar esta propiedad.");
        }

        if (request.getDireccion() != null) propiedad.setDireccion(request.getDireccion());
        if (request.getPrecio() != null) propiedad.setPrecio(request.getPrecio());
        if (request.getCantidadAmbientes() != null) propiedad.setCantidadAmbientes(request.getCantidadAmbientes());
        if (request.getMetrosTotales() != null) propiedad.setMetrosTotales(request.getMetrosTotales());
        if (request.getMetrosCubiertos() != null) propiedad.setMetrosCubiertos(request.getMetrosCubiertos());
        if (request.getTipoVivienda() != null) propiedad.setTipoVivienda(request.getTipoVivienda());
        if (request.getZona() != null) propiedad.setZona(request.getZona());
        if (request.getObservaciones() != null) propiedad.setObservaciones(request.getObservaciones());

        return propiedadRepository.save(propiedad);
    }

    public List<EventoOperacionResumenDTO> obtenerEventosDePropiedad(Long propiedadId, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para ver esta propiedad.");
        }

        return eventoOperacionRepository.findByPropiedadIdOrderByFechaDesc(propiedadId).stream()
                .map(EventoOperacionResumenDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
