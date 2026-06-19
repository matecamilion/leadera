package com.leadera.leadera.service;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.entity.EventoOperacion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.dto.BusquedaDTO;
import com.leadera.leadera.dto.CrearEventoRequest;
import com.leadera.leadera.dto.CrearOperacionRequest;
import com.leadera.leadera.dto.EventoOperacionDTO;
import com.leadera.leadera.dto.OperacionDTO;
import com.leadera.leadera.dto.OperacionPipelineDTO;
import com.leadera.leadera.mapper.EventoOperacionMapper;
import com.leadera.leadera.mapper.PropiedadMapper;
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
    public OperacionDTO crearOperacion(Long leadId, CrearOperacionRequest request, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con id: " + leadId));

        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        if (lead.getAgente() == null || !lead.getAgente().getEmail().equals(emailPropietario)) {
            throw new UnauthorizedActionException("No tenés permiso para crear operaciones en este lead");
        }

        Operacion operacion = new Operacion();
        operacion.setLead(lead);
        operacion.setAgente(lead.getAgente());
        operacion.setTitulo(request.titulo());
        operacion.setTipoOperacion(request.tipoOperacion());
        operacion.setDescripcion(request.descripcion());
        operacion.setEstadoOperacion(EstadoOperacion.PUBLICADA);
        operacion.setFechaCreacion(LocalDateTime.now(zonaHoraria));

        if (request.tipoOperacion() == TipoOperacion.VENTA) {
            asociarPropiedadAVenta(operacion, request, leadId);
        }

        if (request.tipoOperacion() == TipoOperacion.COMPRA) {
            asociarBusquedaACompra(operacion, request);
        }

        Operacion guardada = operacionRepository.save(operacion);

        if (guardada.getTipoOperacion() == TipoOperacion.VENTA && guardada.getPropiedad() != null) {
            sincronizarEstadoPropiedad(guardada.getPropiedad());
        }

        return toDTO(guardada);
    }

    private void asociarPropiedadAVenta(Operacion operacion, CrearOperacionRequest request, Long leadId) {
        if (request.propiedad() == null || request.propiedad().id() == null) {
            throw new BadRequestException("Una operación de venta debe tener una propiedad asociada");
        }

        Long propiedadId = request.propiedad().id();

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

    private void asociarBusquedaACompra(Operacion operacion, CrearOperacionRequest request) {
        if (request.busqueda() == null) {
            throw new BadRequestException("Una operación de compra debe tener una búsqueda asociada");
        }

        CrearOperacionRequest.BusquedaData data = request.busqueda();
        Busqueda busqueda = new Busqueda();
        busqueda.setPrecioMin(data.precioMin());
        busqueda.setPrecioMax(data.precioMax());
        busqueda.setCantidadAmbientes(data.cantidadAmbientes());
        busqueda.setMetrosTotales(data.metrosTotales());
        busqueda.setMetrosCubiertos(data.metrosCubiertos());
        busqueda.setMetrosDescubiertos(data.metrosDescubiertos());
        busqueda.setTipoVivienda(data.tipoVivienda());
        busqueda.setZona(data.zona());
        busqueda.setObservaciones(data.observaciones());

        operacion.setBusqueda(busquedaRepository.save(busqueda));
        operacion.setPropiedad(null);
    }

    public List<OperacionDTO> obtenerOperacionesDePropiedad(Long propiedadId, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la propiedad con id: " + propiedadId));

        Agente propietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente);
        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getId().equals(propietario.getId())) {
            throw new UnauthorizedActionException("No tenés permiso para ver las operaciones de esta propiedad.");
        }

        return operacionRepository.findByPropiedadId(propiedadId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OperacionDTO> obtenerOperacionesDelLead(Long leadId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailPropietario)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OperacionDTO> obtenerOperacionesAbiertasDelLead(Long leadId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByLeadIdAndAgenteEmailAndEstadoOperacionNotIn(
                leadId,
                emailPropietario,
                List.of(EstadoOperacion.CERRADA_GANADA, EstadoOperacion.CANCELADA)
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public OperacionDTO obtenerOperacionPorId(Long leadId, Long operacionId, String emailAgente) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        return operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailPropietario)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para verla"));
    }

    @Transactional
    public OperacionDTO cambiarEstadoOperacion(
            Long leadId,
            Long operacionId,
            EstadoOperacion nuevoEstado,
            String emailAgente
    ) {
        String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
        Operacion operacion = operacionRepository.findByIdAndLeadIdAndAgenteEmail(
                operacionId, leadId, emailPropietario
        ).orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para modificarla"));

        return toDTO(aplicarCambioDeEstado(operacion, nuevoEstado));
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

    private OperacionDTO toDTO(Operacion operacion) {
        return new OperacionDTO(
                operacion.getId(),
                operacion.getTitulo(),
                operacion.getTipoOperacion(),
                operacion.getEstadoOperacion(),
                operacion.getDescripcion(),
                operacion.getFechaCreacion(),
                operacion.getFechaCierre(),
                operacion.getFechaProximoSeguimiento(),
                PropiedadMapper.toDTO(operacion.getPropiedad()),
                toBusquedaDTO(operacion.getBusqueda()),
                operacion.getLead() != null ? operacion.getLead().getId() : null,
                operacion.getMontoOperacion()
        );
    }

    private BusquedaDTO toBusquedaDTO(com.leadera.leadera.entity.Busqueda busqueda) {
        if (busqueda == null) return null;
        return new BusquedaDTO(
                busqueda.getId(),
                busqueda.getPrecioMin(),
                busqueda.getPrecioMax(),
                busqueda.getCantidadAmbientes(),
                busqueda.getMetrosTotales(),
                busqueda.getMetrosCubiertos(),
                busqueda.getMetrosDescubiertos(),
                busqueda.getTipoVivienda(),
                busqueda.getZona(),
                busqueda.getObservaciones()
        );
    }
}