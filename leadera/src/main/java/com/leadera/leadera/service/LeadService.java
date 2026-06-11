package com.leadera.leadera.service;

import com.leadera.leadera.dto.ActividadRecienteDTO;
import com.leadera.leadera.dto.AgenteDashboardDTO;
import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.dto.LeadResumenDTO;
import com.leadera.leadera.dto.InteraccionDTO;
import com.leadera.leadera.dto.LeadEquipoDTO;
import com.leadera.leadera.dto.LeadsHoyResponse;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.mapper.InteraccionMapper;
import com.leadera.leadera.mapper.LeadMapper;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final InteraccionRepository interaccionRepository;
    private final OperacionRepository operacionRepository;
    private final AgenteContextResolver agenteContextResolver;
    private final CacheManager cacheManager;
    private final ZoneId zonaHoraria;

    @Value("${leadera.prioritarios.dias-sin-contacto:3}")
    private int diasSinContactoPrioritario;

    public LeadService(LeadRepository leadRepository,
                       InteraccionRepository interaccionRepository,
                       OperacionRepository operacionRepository,
                       AgenteContextResolver agenteContextResolver,
                       CacheManager cacheManager,
                       ZoneId zonaHoraria) {
        this.leadRepository = leadRepository;
        this.interaccionRepository = interaccionRepository;
        this.operacionRepository = operacionRepository;
        this.agenteContextResolver = agenteContextResolver;
        this.cacheManager = cacheManager;
        this.zonaHoraria = zonaHoraria;
    }

    // Email del propietario efectivo del que pide: para un asistente es el de su
    // supervisor; para los demás, el suyo. Todas las queries por agente filtran
    // por este email para que el asistente vea/opere los leads de su supervisor.
    private String emailPropietario(String email) {
        return agenteContextResolver.resolverPropietarioPorEmail(email).getEmail();
    }

    // True si el lead pertenece al propietario efectivo del que pide.
    private boolean esPropietario(Lead lead, String email) {
        Agente propietario = agenteContextResolver.resolverPropietarioPorEmail(email);
        return lead.getAgente().getId().equals(propietario.getId());
    }


    // El lead se crea SIEMPRE a nombre del propietario efectivo: si quien lo carga
    // es un asistente, el dueño es su supervisor. Por eso la eviction de stats es
    // programática contra el id del PROPIETARIO (no del actor); con @CacheEvict por
    // el email del autenticado las stats del supervisor quedarían stale.
    public LeadResponseDTO crearLead(CrearLeadRequest request, String email) {

        Agente actor = agenteContextResolver.resolverActor(email);
        Agente propietario = agenteContextResolver.resolverPropietario(actor);

        validarDuplicadosEnInmobiliaria(propietario, request.getTelefono(), request.getEmail(), null);

        Lead lead = LeadMapper.toEntity(request);
        if (lead.getEstado() == null) {
            lead.setEstado(EstadoLead.FRIO);
        }
        lead.setAgente(propietario);
        lead.setFechaEntrada(LocalDateTime.now(zonaHoraria));

        LeadResponseDTO creado = LeadMapper.toDTO(leadRepository.save(lead));

        Cache cache = cacheManager.getCache("estadisticasAgente");
        if (cache != null) {
            cache.evict(propietario.getId());
        }

        return creado;
    }

    public List<Lead> obtenerLeadsPorAgente(String email) {
        return leadRepository.findByAgenteEmail(emailPropietario(email));
    }

    public List<Lead> obtenerLeads() {
        return leadRepository.findAll();
    }

    public List<Lead> obtenerLeadsPorEstado(EstadoLead estado, String email) {
        // Filtra por el propietario efectivo (supervisor del asistente, o uno mismo).
        return leadRepository.findByEstadoAndAgenteEmail(estado, emailPropietario(email));
    }

    public List<Lead> obtenerLeadsSinContacto(String email) {
        // Este es clave para la sección de "Nuevos"
        return leadRepository.findByUltimoContactoIsNullAndAgenteEmail(emailPropietario(email));
    }

    public List<Lead> obtenerLeadsInactivos(int dias, String email) {
        LocalDateTime fechaLimite = LocalDateTime.now(zonaHoraria).minusDays(dias);
        return leadRepository.findByUltimoContactoBeforeAndUltimoContactoIsNotNullAndAgenteEmail(fechaLimite, emailPropietario(email));
    }


    public LeadDetalleResponse obtenerLeadsPorId(Long id, String email) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + id));

        if (!esPropietario(lead, email)) {
            throw new UnauthorizedActionException("No tenés permiso para ver este lead.");
        }

        return LeadMapper.toDetalleResponse(lead);
    }

    public List<Lead> obtenerLeadsPrioritarios(int dias, String email) {
        LocalDateTime fechaLimite = LocalDateTime.now(zonaHoraria).minusDays(dias);
        return leadRepository.findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead.CALIENTE, fechaLimite, emailPropietario(email));
    }

    public List<InteraccionDTO> obtenerHistorialInteracciones(Long leadId, String emailAgente) {

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        if (!esPropietario(lead, emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para ver las interacciones de este lead.");
        }

        return lead.getInteracciones().stream()
                .map(InteraccionMapper::toDTO)
                .toList();
    }



    // El Lead retornado ya tiene el agente cargado (se accede a su email arriba),
    // así que tomamos el id desde #result sin query extra.
    @CacheEvict(value = "estadisticasAgente", key = "#result.agente.id")
    public Lead cambiarEstado(Long id, EstadoLead nuevoEstado, String emailAgente) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!esPropietario(lead, emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

        lead.setEstado(nuevoEstado);
        return leadRepository.save(lead);
    }


    public LeadsHoyResponse obtenerLeadsDeHoy(String email) {
        // El "home" del asistente muestra los leads de su supervisor.
        String emailDuenio = emailPropietario(email);
        LocalDateTime ahora = LocalDateTime.now(zonaHoraria);
        LocalDateTime inicioHoy = ahora.toLocalDate().atStartOfDay();

        List<Lead> nuevos = leadRepository.findByUltimoContactoIsNullAndAgenteEmailAndEstadoNot(emailDuenio, EstadoLead.INACTIVO);

        LocalDateTime fechaLimitePrioritarios = ahora.minusDays(diasSinContactoPrioritario);
        List<Lead> prioritarios = leadRepository.findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead.CALIENTE, fechaLimitePrioritarios, emailDuenio);

        // Incluye todos los seguimientos programados para hoy aunque la hora exacta
        // sea futura dentro del mismo día (ej: agendado 18:00, el usuario entra 09:00).
        LocalDateTime finDeHoy = inicioHoy.plusDays(1);
        List<Lead> seguimientos = leadRepository.findSeguimientosPendientes(
                finDeHoy, emailDuenio, EstadoLead.INACTIVO
        );

        // Dedup: un lead que ya está en "prioritarios" no debe duplicarse en "seguimientos".
        // Prioritario manda porque marca un atraso mayor al umbral configurado
        // (leadera.prioritarios.dias-sin-contacto) y requiere atención inmediata.
        Set<Long> idsPrioritarios = prioritarios.stream().map(Lead::getId).collect(Collectors.toSet());
        List<Lead> seguimientosUnicos = seguimientos.stream()
                .filter(l -> !idsPrioritarios.contains(l.getId()))
                .toList();

        List<Lead> yaContactados = leadRepository.findByUltimoContactoAfterAndAgenteEmail(inicioHoy, emailDuenio);

        int totalTareas = nuevos.size() + prioritarios.size() + seguimientosUnicos.size() + yaContactados.size();
        int completadas = yaContactados.size();

        return new LeadsHoyResponse(
                nuevos.stream().map(LeadMapper::toHoyDTO).toList(),
                prioritarios.stream().map(LeadMapper::toHoyDTO).toList(),
                seguimientosUnicos.stream().map(LeadMapper::toHoyDTO).toList(),
                yaContactados.stream().map(LeadMapper::toHoyDTO).toList(),
                totalTareas,
                completadas
        );
    }

    // El Lead retornado ya tiene el agente cargado (se accede a su email arriba),
    // así que tomamos el id desde #result sin query extra.
    @CacheEvict(value = "estadisticasAgente", key = "#result.agente.id")
    public Lead establecerLeadInactivo(Long id, String emailAgente) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!esPropietario(lead, emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

         lead.setEstado(EstadoLead.INACTIVO);
         return leadRepository.save(lead);
    }

    @Transactional
    public void eliminarLead(Long leadId, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!esPropietario(lead, emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para eliminar este lead.");
        }

        // Tras el check, lead.getAgente() ES el propietario efectivo. El id y el email
        // salen del lead ya cargado: no hace falta query extra al repo de agentes.
        Long agenteId = lead.getAgente().getId();
        String emailPropietario = lead.getAgente().getEmail();

        // Interacciones y propiedades caen solas por el cascade = ALL de Lead. Las operaciones
        // NO están mapeadas como @OneToMany en Lead y tienen FK lead_id NOT NULL, así que el
        // cascade de JPA no las alcanza: hay que borrarlas explícitamente antes del lead (sus
        // eventos se eliminan por el cascade propio de Operacion). Se borran primero para no
        // violar la FK operacion -> propiedad cuando el cascade del lead borra las propiedades.
        // Se filtra por el email del PROPIETARIO (dueño de las operaciones), no por el del actor:
        // si quien elimina es un asistente, las operaciones pertenecen a su supervisor.
        List<Operacion> operaciones = operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailPropietario);
        operacionRepository.deleteAll(operaciones);

        leadRepository.delete(lead);

        // Evict acotado al agente dueño, con la misma key que @Cacheable en obtenerEstadisticasAgente.
        // No se usa @CacheEvict porque la firma recibe el email y el id se resuelve desde el lead
        // cargado (una expresión SpEL en la anotación obligaría a una query extra de agente).
        Cache cache = cacheManager.getCache("estadisticasAgente");
        if (cache != null) {
            cache.evict(agenteId);
        }
    }

    @Cacheable(value = "estadisticasAgente", key = "#agenteId")
    public AgenteDashboardDTO obtenerEstadisticasAgente(Long agenteId){
        long activos = leadRepository.countByAgenteIdAndEstadoNot(agenteId, EstadoLead.INACTIVO);
        long calientes = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.CALIENTE);
        long tibios = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.TIBIO);
        long frios = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.FRIO);
        long ganadosMes = operacionRepository.countOperacionesGanadasDelMes(agenteId);
        long nuevosDelMes = leadRepository.countIngresosDelMes(agenteId);
        long perdidos = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.INACTIVO);

        long interacciones7d = interaccionRepository.countInteraccionesDesde(
                agenteId, LocalDateTime.now(zonaHoraria).minusDays(7)
        );

        double tasaConversion = nuevosDelMes > 0
                ? Math.round((double) ganadosMes / nuevosDelMes * 100 * 10.0) / 10.0
                : 0.0;

        List<Lead> leads = leadRepository.findLeadsConFechaEntrada(agenteId);
        double tiempoRespuesta = leads.stream()
                .mapToLong(lead -> {
                    LocalDateTime primera = interaccionRepository.findPrimeraInteraccion(lead.getId());
                    if (primera == null) return -1L;
                    return ChronoUnit.HOURS.between(lead.getFechaEntrada(), primera);
                })
                .filter(h -> h >= 0)
                .average()
                .orElse(0.0);
        double tiempoRespuestaDias = Math.round(tiempoRespuesta / 24 * 10.0) / 10.0;

        return new AgenteDashboardDTO(activos, calientes, tibios, frios, ganadosMes,
                nuevosDelMes, perdidos, interacciones7d,
                tasaConversion, tiempoRespuestaDias);
    }

    public Lead editarInfoContacto(Long leadId, String nuevoTelefono, String nuevoEmail, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!esPropietario(lead, emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para editar este lead.");
        }

        // El ownership ya está validado: lead.getAgente() es el propietario efectivo.
        validarDuplicadosEnInmobiliaria(lead.getAgente(), nuevoTelefono, nuevoEmail, leadId);

        lead.setTelefono(nuevoTelefono);
        lead.setEmail(nuevoEmail);

        return leadRepository.save(lead);
    }


    public List<ActividadRecienteDTO> obtenerActividadReciente(Long agenteId, int limit) {
        return interaccionRepository.findUltimasInteracciones(agenteId, PageRequest.of(0, limit))
                .stream()
                .map(ActividadRecienteDTO::fromEntity)
                .toList();
    }

    public List<LeadResumenDTO> obtenerResumenLeadsPorAgente(String email) {
        List<Lead> leads = leadRepository.findByAgenteEmailConInteracciones(emailPropietario(email));
        Map<Long, Map<TipoOperacion, Long>> conteos = cargarConteosOperaciones(
                leads.stream().map(Lead::getId).toList());
        return leads.stream().map(lead -> toResumenDTO(lead, conteos)).toList();
    }

    // Unicidad de teléfono y email a nivel INMOBILIARIA (no por agente): si el
    // duplicado pertenece a otro agente del equipo, el mensaje lo nombra para que
    // puedan coordinar. leadIdExcluido permite reutilizarlo en la edición.
    private void validarDuplicadosEnInmobiliaria(Agente agente, String telefono, String email, Long leadIdExcluido) {
        Long inmobiliariaId = agente.getInmobiliaria().getId();

        if (telefono != null && !telefono.isBlank()) {
            Optional<Lead> duplicado = (leadIdExcluido == null)
                    ? leadRepository.findFirstByAgente_Inmobiliaria_IdAndTelefono(inmobiliariaId, telefono)
                    : leadRepository.findFirstByAgente_Inmobiliaria_IdAndTelefonoAndIdNot(inmobiliariaId, telefono, leadIdExcluido);
            duplicado.ifPresent(l -> {
                throw new DuplicateResourceException(mensajeDuplicado("teléfono", l, agente));
            });
        }

        if (email != null && !email.isBlank()) {
            Optional<Lead> duplicado = (leadIdExcluido == null)
                    ? leadRepository.findFirstByAgente_Inmobiliaria_IdAndEmail(inmobiliariaId, email)
                    : leadRepository.findFirstByAgente_Inmobiliaria_IdAndEmailAndIdNot(inmobiliariaId, email, leadIdExcluido);
            duplicado.ifPresent(l -> {
                throw new DuplicateResourceException(mensajeDuplicado("email", l, agente));
            });
        }
    }

    private String mensajeDuplicado(String campo, Lead duplicado, Agente solicitante) {
        Agente duenoDelDuplicado = duplicado.getAgente();
        if (duenoDelDuplicado.getId().equals(solicitante.getId())) {
            return "Ya tenés un lead con ese " + campo + ".";
        }
        return "Ya existe un lead con ese " + campo + " en tu inmobiliaria (agente: "
                + duenoDelDuplicado.getNombre() + " " + duenoDelDuplicado.getApellido() + ")";
    }

    // Listado read-only de todos los leads de la inmobiliaria para la vista
    // "Mi equipo" del dueño. Reutiliza el armado de LeadResumenDTO y le suma
    // el agente dueño de cada lead.
    public Page<LeadEquipoDTO> obtenerResumenLeadsPorInmobiliaria(Long inmobiliariaId, Pageable pageable) {
        Pageable efectivo = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                 Sort.by(Sort.Direction.DESC, "fechaEntrada"));
        Page<Lead> page = leadRepository.findByInmobiliariaConInteraccionesPaginado(inmobiliariaId, efectivo);
        Map<Long, Map<TipoOperacion, Long>> conteos = cargarConteosOperaciones(
                page.getContent().stream().map(Lead::getId).toList());
        return page.map(lead -> new LeadEquipoDTO(
                toResumenDTO(lead, conteos),
                lead.getAgente().getId(),
                lead.getAgente().getNombre(),
                lead.getAgente().getApellido()
        ));
    }

    public Page<LeadResumenDTO> obtenerResumenLeadsPorAgente(String email, Pageable pageable) {
        Pageable efectivo = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                 Sort.by(Sort.Direction.DESC, "fechaEntrada"));
        Page<Lead> page = leadRepository.findByAgenteEmailConInteraccionesPaginado(emailPropietario(email), efectivo);
        Map<Long, Map<TipoOperacion, Long>> conteos = cargarConteosOperaciones(
                page.getContent().stream().map(Lead::getId).toList());
        return page.map(lead -> toResumenDTO(lead, conteos));
    }

    // Pre-carga los conteos de operaciones por tipo de todos los leads en una sola query.
    // Sin esto, toResumenDTO disparaba 3 queries por lead (N+1).
    private Map<Long, Map<TipoOperacion, Long>> cargarConteosOperaciones(List<Long> leadIds) {
        if (leadIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<TipoOperacion, Long>> conteos = new HashMap<>();
        for (Object[] fila : operacionRepository.countByLeadIdsGroupedByTipo(leadIds)) {
            Long leadId = (Long) fila[0];
            TipoOperacion tipo = (TipoOperacion) fila[1];
            long cantidad = ((Number) fila[2]).longValue();
            conteos.computeIfAbsent(leadId, k -> new EnumMap<>(TipoOperacion.class)).put(tipo, cantidad);
        }
        return conteos;
    }

    private LeadResumenDTO toResumenDTO(Lead lead, Map<Long, Map<TipoOperacion, Long>> conteos) {
        Map<TipoOperacion, Long> porTipo = conteos.getOrDefault(lead.getId(), Map.of());
        long ventas = porTipo.getOrDefault(TipoOperacion.VENTA, 0L);
        long compras = porTipo.getOrDefault(TipoOperacion.COMPRA, 0L);
        long alquileres = porTipo.getOrDefault(TipoOperacion.ALQUILER, 0L);
        long interacciones = lead.getInteracciones().size();
        String ultimaInteraccion = lead.getInteracciones().isEmpty() ? null :
                lead.getInteracciones().get(lead.getInteracciones().size() - 1).getDetalle();
        return new LeadResumenDTO(
                lead.getId(), lead.getNombre(), lead.getApellido(),
                lead.getTelefono(), lead.getEmail(), lead.getEstado(),
                lead.getOrigen(),
                lead.getFechaEntrada(), lead.getUltimoContacto(),
                lead.getFechaProximoSeguimiento(),
                ventas, compras, alquileres, interacciones, ultimaInteraccion
        );
    }

}
