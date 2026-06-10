package com.leadera.leadera.service;

import com.leadera.leadera.dto.CompradorPotencialDTO;
import com.leadera.leadera.dto.CrearPropiedadRequest;
import com.leadera.leadera.dto.EditarPropiedadRequest;
import com.leadera.leadera.dto.EventoOperacionResumenDTO;
import com.leadera.leadera.dto.PropiedadDTO;
import com.leadera.leadera.dto.PropiedadResumenDTO;
import com.leadera.leadera.mapper.PropiedadMapper;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.EventoOperacionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropiedadService {
    private final PropiedadRepository propiedadRepository;
    private final LeadRepository leadRepository;
    private final EventoOperacionRepository eventoOperacionRepository;
    private final OperacionRepository operacionRepository;
    private final AgenteAutenticadoService agenteAutenticadoService;
    private final ZoneId zonaHoraria;

    public PropiedadService(PropiedadRepository propiedadRepository,
                            LeadRepository leadRepository,
                            EventoOperacionRepository eventoOperacionRepository,
                            OperacionRepository operacionRepository,
                            AgenteAutenticadoService agenteAutenticadoService,
                            ZoneId zonaHoraria) {
        this.propiedadRepository = propiedadRepository;
        this.leadRepository = leadRepository;
        this.eventoOperacionRepository = eventoOperacionRepository;
        this.operacionRepository = operacionRepository;
        this.agenteAutenticadoService = agenteAutenticadoService;
        this.zonaHoraria = zonaHoraria;
    }

    public PropiedadDTO agregarPropiedad(Long leadId, CrearPropiedadRequest request, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

        // Entidad construida acá y no bindeada del request: un id en el body
        // no puede convertir el save en un merge sobre una propiedad ajena.
        Propiedad propiedad = new Propiedad();
        propiedad.setDireccion(request.direccion());
        propiedad.setPrecio(request.precio());
        propiedad.setCantidadAmbientes(request.cantidadAmbientes());
        propiedad.setMetrosTotales(request.metrosTotales());
        propiedad.setMetrosCubiertos(request.metrosCubiertos());
        propiedad.setTipoVivienda(request.tipoVivienda());
        propiedad.setZona(request.zona());
        propiedad.setObservaciones(request.observaciones());
        propiedad.setLead(lead);
        propiedad.setFechaPublicacion(LocalDateTime.now(zonaHoraria));
        return PropiedadMapper.toDTO(propiedadRepository.save(propiedad));
    }

    public List<PropiedadDTO> obtenerPropiedadesDeLead(Long leadId, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (lead.getAgente() == null || !lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para ver las propiedades de este lead.");
        }

        return propiedadRepository.findByLeadId(leadId).stream()
                .map(PropiedadMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PropiedadResumenDTO> obtenerPropiedadesDelAgente(String emailAgente) {
        return propiedadRepository.findByAgenteEmail(emailAgente).stream()
                .map(PropiedadResumenDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PropiedadDTO actualizarEstado(Long id, String estado, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar esta propiedad.");
        }

        try {
            propiedad.setEstado(EstadoPropiedad.valueOf(estado));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado de propiedad inválido: " + estado);
        }
        return PropiedadMapper.toDTO(propiedadRepository.save(propiedad));
    }

    public PropiedadDTO obtenerPorId(Long id, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para ver esta propiedad.");
        }

        return PropiedadMapper.toDTO(propiedad);
    }

    public PropiedadDTO editarPropiedad(Long propiedadId, EditarPropiedadRequest request, String emailAgente) {
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

        return PropiedadMapper.toDTO(propiedadRepository.save(propiedad));
    }

    // ---------- Matching ----------

    public List<CompradorPotencialDTO> buscarCompradoresPotenciales(Long propiedadId, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para ver esta propiedad.");
        }

        if (propiedad.getEstado() != EstadoPropiedad.DISPONIBLE) {
            return List.of();
        }

        // Matching compartido: cruza las búsquedas de COMPRA de toda la
        // inmobiliaria, no solo las del agente que consulta.
        Agente agenteActual = agenteAutenticadoService.obtenerPorEmail(emailAgente);
        Long inmobiliariaId = agenteActual.getInmobiliaria().getId();

        return operacionRepository.findCandidatosParaMatching(inmobiliariaId).stream()
                .filter(op -> coincide(propiedad, op.getBusqueda()))
                .map(op -> toCompradorDTO(op, agenteActual.getId()))
                .toList();
    }

    private boolean coincide(Propiedad p, Busqueda b) {
        if (b == null) return false;

        // tipoVivienda — obligatorio
        if (p.getTipoVivienda() == null || b.getTipoVivienda() == null) return false;
        if (p.getTipoVivienda() != b.getTipoVivienda()) return false;

        // zona — obligatorio, case-insensitive con contains bidireccional
        if (p.getZona() == null || b.getZona() == null) return false;
        String zp = p.getZona().toLowerCase().trim();
        String zb = b.getZona().toLowerCase().trim();
        if (zp.isEmpty() || zb.isEmpty()) return false;
        if (!zp.contains(zb) && !zb.contains(zp)) return false;

        // precio — opcional (precioMin, precioMax)
        BigDecimal precio = p.getPrecio();
        if (b.getPrecioMin() != null) {
            if (precio == null || precio.compareTo(b.getPrecioMin()) < 0) return false;
        }
        if (b.getPrecioMax() != null) {
            if (precio == null || precio.compareTo(b.getPrecioMax()) > 0) return false;
        }

        // ambientes — opcional, propiedad >= búsqueda
        if (b.getCantidadAmbientes() != null) {
            if (p.getCantidadAmbientes() == null
                    || p.getCantidadAmbientes() < b.getCantidadAmbientes()) {
                return false;
            }
        }

        // metros totales — opcional, propiedad >= búsqueda
        if (b.getMetrosTotales() != null) {
            if (p.getMetrosTotales() == null
                    || p.getMetrosTotales() < b.getMetrosTotales()) {
                return false;
            }
        }

        return true;
    }

    // Sin teléfono ni email del lead a propósito: el matching es compartido a
    // nivel inmobiliaria y los datos de contacto de leads ajenos no se exponen.
    private CompradorPotencialDTO toCompradorDTO(Operacion op, Long agenteActualId) {
        Lead lead = op.getLead();
        Busqueda b = op.getBusqueda();
        Agente duenoDelLead = op.getAgente();
        return new CompradorPotencialDTO(
                lead.getId(),
                lead.getNombre(),
                lead.getApellido(),
                lead.getEstado(),
                op.getId(),
                b.getZona(),
                b.getTipoVivienda() != null ? b.getTipoVivienda().name() : null,
                b.getPrecioMin(),
                b.getPrecioMax(),
                b.getCantidadAmbientes(),
                b.getMetrosTotales(),
                duenoDelLead.getId(),
                duenoDelLead.getNombre(),
                duenoDelLead.getApellido(),
                duenoDelLead.getId().equals(agenteActualId)
        );
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
