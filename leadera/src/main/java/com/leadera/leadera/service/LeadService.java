package com.leadera.leadera.service;

import com.leadera.leadera.dto.ActividadRecienteDTO;
import com.leadera.leadera.dto.AgenteDashboardDTO;
import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.dto.LeadResumenDTO;
import com.leadera.leadera.dto.InteraccionDTO;
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
import com.leadera.leadera.repository.AgenteRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final AgenteRepository agenteRepository;
    private final InteraccionRepository interaccionRepository;
    private final OperacionRepository operacionRepository;
    private final CacheManager cacheManager;
    private final ZoneId zonaHoraria;

    @Value("${leadera.prioritarios.dias-sin-contacto:3}")
    private int diasSinContactoPrioritario;

    public LeadService(LeadRepository leadRepository,
                       AgenteRepository agenteRepository,
                       InteraccionRepository interaccionRepository,
                       OperacionRepository operacionRepository,
                       CacheManager cacheManager,
                       ZoneId zonaHoraria) {
        this.leadRepository = leadRepository;
        this.agenteRepository = agenteRepository;
        this.interaccionRepository = interaccionRepository;
        this.operacionRepository = operacionRepository;
        this.cacheManager = cacheManager;
        this.zonaHoraria = zonaHoraria;
    }


    // Evict acotado al agente dueño del lead (no a todo el cache). El método recibe el
    // email, así que resolvemos su id desde el repositorio para que la key coincida con
    // la de @Cacheable en obtenerEstadisticasAgente (key="#agenteId").
    @CacheEvict(value = "estadisticasAgente", key = "@agenteRepository.findByEmail(#email).get().id")
    public LeadResponseDTO crearLead(CrearLeadRequest request, String email) {

        Agente agente = agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && leadRepository.existsByAgenteEmailAndEmail(email, request.getEmail())) {
            throw new DuplicateResourceException("Ya tenés un lead con ese email");
        }

        Lead lead = LeadMapper.toEntity(request);
        if (lead.getEstado() == null) {
            lead.setEstado(EstadoLead.FRIO);
        }
        lead.setAgente(agente);
        lead.setFechaEntrada(LocalDateTime.now(zonaHoraria));

        return LeadMapper.toDTO(leadRepository.save(lead));
    }

    public List<Lead> obtenerLeadsPorAgente(String email) {
        return leadRepository.findByAgenteEmail(email);
    }

    public List<Lead> obtenerLeads() {
        return leadRepository.findAll();
    }

    public List<Lead> obtenerLeadsPorEstado(EstadoLead estado, String email) { // <--- Agregamos email
        // Usamos el nuevo método del repository que filtra por ambas cosas
        return leadRepository.findByEstadoAndAgenteEmail(estado, email);
    }

    public List<Lead> obtenerLeadsSinContacto(String email) { // <--- Agregamos email
        // Este es clave para la sección de "Nuevos"
        return leadRepository.findByUltimoContactoIsNullAndAgenteEmail(email);
    }

    public List<Lead> obtenerLeadsInactivos(int dias, String email) { // <--- Agregamos email
        LocalDateTime fechaLimite = LocalDateTime.now(zonaHoraria).minusDays(dias);
        // Usamos el nuevo método del repo con filtro de email
        return leadRepository.findByUltimoContactoBeforeAndUltimoContactoIsNotNullAndAgenteEmail(fechaLimite, email);
    }


    public LeadDetalleResponse obtenerLeadsPorId(Long id, String email) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + id));

        if (!lead.getAgente().getEmail().equals(email)) {
            throw new UnauthorizedActionException("No tenés permiso para ver este lead.");
        }

        return LeadMapper.toDetalleResponse(lead);
    }

    public List<Lead> obtenerLeadsPrioritarios(int dias, String email) { // <--- Agregamos email
        LocalDateTime fechaLimite = LocalDateTime.now(zonaHoraria).minusDays(dias);
        // Usamos el nuevo método del repo con filtro de email
        return leadRepository.findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead.CALIENTE, fechaLimite, email);
    }

    public List<InteraccionDTO> obtenerHistorialInteracciones(Long leadId, String emailAgente) {

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
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

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

        lead.setEstado(nuevoEstado);
        return leadRepository.save(lead);
    }


    public LeadsHoyResponse obtenerLeadsDeHoy(String email) {
        LocalDateTime ahora = LocalDateTime.now(zonaHoraria);
        LocalDateTime inicioHoy = ahora.toLocalDate().atStartOfDay();

        List<Lead> nuevos = leadRepository.findByUltimoContactoIsNullAndAgenteEmailAndEstadoNot(email, EstadoLead.INACTIVO);

        LocalDateTime fechaLimitePrioritarios = ahora.minusDays(diasSinContactoPrioritario);
        List<Lead> prioritarios = leadRepository.findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead.CALIENTE, fechaLimitePrioritarios, email);

        // Incluye todos los seguimientos programados para hoy aunque la hora exacta
        // sea futura dentro del mismo día (ej: agendado 18:00, el usuario entra 09:00).
        LocalDateTime finDeHoy = inicioHoy.plusDays(1);
        List<Lead> seguimientos = leadRepository.findSeguimientosPendientes(
                finDeHoy, email, EstadoLead.INACTIVO
        );

        // Dedup: un lead que ya está en "prioritarios" no debe duplicarse en "seguimientos".
        // Prioritario manda porque marca un atraso mayor al umbral configurado
        // (leadera.prioritarios.dias-sin-contacto) y requiere atención inmediata.
        Set<Long> idsPrioritarios = prioritarios.stream().map(Lead::getId).collect(Collectors.toSet());
        List<Lead> seguimientosUnicos = seguimientos.stream()
                .filter(l -> !idsPrioritarios.contains(l.getId()))
                .toList();

        List<Lead> yaContactados = leadRepository.findByUltimoContactoAfterAndAgenteEmail(inicioHoy, email);

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

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

         lead.setEstado(EstadoLead.INACTIVO);
         return leadRepository.save(lead);
    }

    @Transactional
    public void eliminarLead(Long leadId, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para eliminar este lead.");
        }

        // El agenteId sale del lead ya cargado: no hace falta una query extra al repo de agentes.
        Long agenteId = lead.getAgente().getId();

        // Interacciones y propiedades caen solas por el cascade = ALL de Lead. Las operaciones
        // NO están mapeadas como @OneToMany en Lead y tienen FK lead_id NOT NULL, así que el
        // cascade de JPA no las alcanza: hay que borrarlas explícitamente antes del lead (sus
        // eventos se eliminan por el cascade propio de Operacion). Se borran primero para no
        // violar la FK operacion -> propiedad cuando el cascade del lead borra las propiedades.
        List<Operacion> operaciones = operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailAgente);
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

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para editar este lead.");
        }

        boolean telefonoDuplicado = leadRepository.existsByAgenteEmailAndTelefonoAndIdNot(emailAgente, nuevoTelefono, leadId);
        if (telefonoDuplicado) {
            throw new DuplicateResourceException("Ya tenés un lead con ese teléfono.");
        }

        boolean emailDuplicado = leadRepository.existsByAgenteEmailAndEmailAndIdNot(emailAgente, nuevoEmail, leadId);
        if (emailDuplicado) {
            throw new DuplicateResourceException("Ya tenés un lead con ese email.");
        }

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
        List<Lead> leads = leadRepository.findByAgenteEmailConInteracciones(email);
        Map<Long, Map<TipoOperacion, Long>> conteos = cargarConteosOperaciones(
                leads.stream().map(Lead::getId).toList());
        return leads.stream().map(lead -> toResumenDTO(lead, conteos)).toList();
    }

    public Page<LeadResumenDTO> obtenerResumenLeadsPorAgente(String email, Pageable pageable) {
        Pageable efectivo = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                 Sort.by(Sort.Direction.DESC, "fechaEntrada"));
        Page<Lead> page = leadRepository.findByAgenteEmailConInteraccionesPaginado(email, efectivo);
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
