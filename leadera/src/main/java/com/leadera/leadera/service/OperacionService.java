package com.leadera.leadera.service;

import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.entity.EventoOperacion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.dto.CrearEventoRequest;
import com.leadera.leadera.dto.EventoOperacionDTO;
import com.leadera.leadera.dto.OperacionPipelineDTO;
import com.leadera.leadera.mapper.EventoOperacionMapper;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.BusquedaRepository;
import com.leadera.leadera.repository.EventoOperacionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OperacionService {

    private final OperacionRepository operacionRepository;
    private final LeadRepository leadRepository;
    private final PropiedadRepository propiedadRepository;
    private final BusquedaRepository busquedaRepository;
    private final EventoOperacionRepository eventoOperacionRepository;
    private final AgenteContextResolver agenteContextResolver;
    private final ZoneId zonaHoraria;

    private static final Map<EstadoOperacion, Set<EstadoOperacion>> TRANSICIONES_VALIDAS = Map.of(
            EstadoOperacion.PUBLICADA,       EnumSet.of(EstadoOperacion.RESERVADA, EstadoOperacion.CANCELADA),
            EstadoOperacion.RESERVADA,       EnumSet.of(EstadoOperacion.PUBLICADA, EstadoOperacion.EN_NEGOCIACION, EstadoOperacion.CANCELADA),
            EstadoOperacion.EN_NEGOCIACION,  EnumSet.of(EstadoOperacion.RESERVADA, EstadoOperacion.CERRADA_GANADA, EstadoOperacion.CANCELADA),
            EstadoOperacion.CERRADA_GANADA,  EnumSet.noneOf(EstadoOperacion.class),
            EstadoOperacion.CANCELADA,       EnumSet.noneOf(EstadoOperacion.class)
    );

    public OperacionService(
            OperacionRepository operacionRepository,
            LeadRepository leadRepository,
            PropiedadRepository propiedadRepository,
            BusquedaRepository busquedaRepository,
            EventoOperacionRepository eventoOperacionRepository,
            AgenteContextResolver agenteContextResolver,
            ZoneId zonaHoraria
    ) {
        this.operacionRepository = operacionRepository;
        this.leadRepository = leadRepository;
        this.propiedadRepository = propiedadRepository;
        this.busquedaRepository = busquedaRepository;
        this.eventoOperacionRepository = eventoOperacionRepository;
        this.agenteContextResolver = agenteContextResolver;
        this.zonaHoraria = zonaHoraria;
    }

    @Transactional
    public Operacion crearOperacion(Long leadId, Operacion operacionRequest, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con id: " + leadId));

        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        if (lead.getAgente() == null || !lead.getAgente().getEmail().equals(emailPropietario)) {
            throw new UnauthorizedActionException("No tenés permiso para crear operaciones en este lead");
        }

        Operacion operacion = new Operacion();

        operacion.setLead(lead);
        operacion.setAgente(lead.getAgente());
        operacion.setTitulo(operacionRequest.getTitulo());
        operacion.setTipoOperacion(operacionRequest.getTipoOperacion());
        operacion.setDescripcion(operacionRequest.getDescripcion());
        operacion.setEstadoOperacion(EstadoOperacion.PUBLICADA);
        operacion.setFechaCreacion(LocalDateTime.now(zonaHoraria));

        if (operacionRequest.getTipoOperacion() == TipoOperacion.VENTA) {
            asociarPropiedadAVenta(operacion, operacionRequest, leadId);
        }

        if (operacionRequest.getTipoOperacion() == TipoOperacion.COMPRA) {
            asociarBusquedaACompra(operacion, operacionRequest);
        }

        Operacion guardada = operacionRepository.save(operacion);

        if (guardada.getTipoOperacion() == TipoOperacion.VENTA && guardada.getPropiedad() != null) {
            sincronizarEstadoPropiedad(guardada.getPropiedad());
        }

        return guardada;
    }

    private void asociarPropiedadAVenta(Operacion operacion, Operacion operacionRequest, Long leadId) {
        if (operacionRequest.getPropiedad() == null || operacionRequest.getPropiedad().getId() == 0) {
            throw new BadRequestException("Una operación de venta debe tener una propiedad asociada");
        }

        Long propiedadId = operacionRequest.getPropiedad().getId();

        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la propiedad con id: " + propiedadId));

        if (propiedad.getLead() == null || !propiedad.getLead().getId().equals(leadId)) {
            throw new BadRequestException("La propiedad no pertenece a este lead");
        }

        if (propiedad.getEstado() == EstadoPropiedad.VENDIDA) {
            throw new BadRequestException("No se puede crear una operación de venta sobre una propiedad ya vendida.");
        }

        operacion.setPropiedad(propiedad);
        operacion.setBusqueda(null);
    }

    private void asociarBusquedaACompra(Operacion operacion, Operacion operacionRequest) {
        if (operacionRequest.getBusqueda() == null) {
            throw new BadRequestException("Una operación de compra debe tener una búsqueda asociada");
        }

        Busqueda busqueda = operacionRequest.getBusqueda();
        busqueda.setId(null);
        Busqueda busquedaGuardada = busquedaRepository.save(busqueda);

        operacion.setBusqueda(busquedaGuardada);
        operacion.setPropiedad(null);
    }

    public List<Operacion> obtenerOperacionesDelLead(Long leadId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailPropietario);
    }

    public List<Operacion> obtenerOperacionesAbiertasDelLead(Long leadId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByLeadIdAndAgenteEmailAndEstadoOperacionNotIn(
                leadId,
                emailPropietario,
                List.of(
                        EstadoOperacion.CERRADA_GANADA,
                        EstadoOperacion.CANCELADA
                )
        );
    }

    public Operacion obtenerOperacionPorId(Long leadId, Long operacionId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailPropietario)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para verla"));
    }


    @Transactional
    public Operacion cambiarEstadoOperacion(
            Long leadId,
            Long operacionId,
            EstadoOperacion nuevoEstado,
            String emailAgente
    ) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        Operacion operacion = operacionRepository.findByIdAndLeadIdAndAgenteEmail(
                operacionId,
                leadId,
                emailPropietario
        ).orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para modificarla"));

        return aplicarCambioDeEstado(operacion, nuevoEstado);
    }

    @Transactional
    public OperacionPipelineDTO cambiarEstadoOperacionPipeline(
            Long operacionId,
            EstadoOperacion nuevoEstado,
            String emailAgente
    ) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        Operacion operacion = operacionRepository.findByIdAndAgenteEmail(operacionId, emailPropietario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la operación o no tenés permiso para modificarla"));

        Operacion actualizada = aplicarCambioDeEstado(operacion, nuevoEstado);
        return OperacionPipelineDTO.fromEntity(actualizada);
    }

    public List<OperacionPipelineDTO> obtenerPipelineDelAgente(String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findPipelineByAgenteEmail(emailPropietario).stream()
                .map(OperacionPipelineDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<OperacionPipelineDTO> obtenerOperacionesCerradasDelAgente(String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findCerradasByAgenteEmail(emailPropietario).stream()
                .map(OperacionPipelineDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EventoOperacionDTO registrarEvento(Long leadId, Long operacionId, CrearEventoRequest request, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        Operacion operacion = operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailPropietario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la operación o no tenés permiso para modificarla"));

        // Entidad construida acá y no bindeada del request: un id en el body
        // no puede convertir el save en un merge sobre un evento ajeno.
        EventoOperacion evento = new EventoOperacion();
        evento.setTipo(request.tipo());
        evento.setDetalle(request.detalle());
        evento.setFecha(LocalDateTime.now(zonaHoraria));
        evento.setOperacion(operacion);

        return EventoOperacionMapper.toDTO(eventoOperacionRepository.save(evento));
    }

    public List<EventoOperacionDTO> obtenerEventos(Long leadId, Long operacionId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailPropietario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la operación o no tenés permiso para verla"));

        return eventoOperacionRepository.findByOperacionIdOrderByFechaDesc(operacionId).stream()
                .map(EventoOperacionMapper::toDTO)
                .collect(Collectors.toList());
    }

    private Operacion aplicarCambioDeEstado(Operacion operacion, EstadoOperacion nuevoEstado) {
        if (nuevoEstado == null) {
            throw new BadRequestException("El estado de la operación no puede ser nulo");
        }

        EstadoOperacion estadoActual = operacion.getEstadoOperacion();

        if (estadoActual == nuevoEstado) {
            return operacion;
        }

        Set<EstadoOperacion> permitidos = TRANSICIONES_VALIDAS.getOrDefault(estadoActual, EnumSet.noneOf(EstadoOperacion.class));
        if (!permitidos.contains(nuevoEstado)) {
            throw new BadRequestException(
                    "Transición no permitida: " + estadoActual + " → " + nuevoEstado);
        }

        if (operacion.getTipoOperacion() == TipoOperacion.VENTA && operacion.getPropiedad() != null) {
            validarConflictoConPropiedad(operacion, nuevoEstado);
        }

        operacion.setEstadoOperacion(nuevoEstado);

        if (nuevoEstado == EstadoOperacion.CERRADA_GANADA || nuevoEstado == EstadoOperacion.CANCELADA) {
            operacion.setFechaCierre(LocalDateTime.now(zonaHoraria));
        } else {
            operacion.setFechaCierre(null);
        }

        Operacion guardada = operacionRepository.save(operacion);

        if (guardada.getTipoOperacion() == TipoOperacion.VENTA && guardada.getPropiedad() != null) {
            sincronizarEstadoPropiedad(guardada.getPropiedad());
        }

        return guardada;
    }

    private void validarConflictoConPropiedad(Operacion operacion, EstadoOperacion nuevoEstado) {
        if (nuevoEstado != EstadoOperacion.RESERVADA && nuevoEstado != EstadoOperacion.CERRADA_GANADA) {
            return;
        }

        Long propiedadId = operacion.getPropiedad().getId();
        List<Operacion> otras = operacionRepository.findByPropiedadId(propiedadId).stream()
                .filter(o -> !o.getId().equals(operacion.getId()))
                .toList();

        boolean otraGanada = otras.stream()
                .anyMatch(o -> o.getEstadoOperacion() == EstadoOperacion.CERRADA_GANADA);
        if (otraGanada) {
            throw new BadRequestException(
                    "Esta propiedad ya tiene otra operación cerrada como ganada.");
        }

        if (nuevoEstado == EstadoOperacion.RESERVADA) {
            boolean otraReservada = otras.stream()
                    .anyMatch(o -> o.getEstadoOperacion() == EstadoOperacion.RESERVADA);
            if (otraReservada) {
                throw new BadRequestException(
                        "Esta propiedad ya está reservada por otra operación activa.");
            }
        }
    }

    private void sincronizarEstadoPropiedad(Propiedad propiedad) {
        if (propiedad == null) return;

        List<Operacion> ops = operacionRepository.findByPropiedadId(propiedad.getId());

        boolean hayGanada = ops.stream()
                .anyMatch(o -> o.getEstadoOperacion() == EstadoOperacion.CERRADA_GANADA);
        boolean hayReservada = ops.stream()
                .anyMatch(o -> o.getEstadoOperacion() == EstadoOperacion.RESERVADA);

        EstadoPropiedad nuevoEstado;
        if (hayGanada) {
            nuevoEstado = EstadoPropiedad.VENDIDA;
        } else if (hayReservada) {
            nuevoEstado = EstadoPropiedad.RESERVADA;
        } else {
            nuevoEstado = EstadoPropiedad.DISPONIBLE;
        }

        if (propiedad.getEstado() != nuevoEstado) {
            propiedad.setEstado(nuevoEstado);
            propiedadRepository.save(propiedad);
        }
    }
}