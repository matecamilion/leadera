package com.leadera.leadera.service;

import com.leadera.leadera.dto.ActividadRecienteDTO;
import com.leadera.leadera.dto.AgenteDashboardDTO;
import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.dto.LeadResumenDTO;
import com.leadera.leadera.dto.LeadsHoyResponse;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.mapper.LeadMapper;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final AgenteRepository agenteRepository;
    private final InteraccionRepository interaccionRepository;
    private final OperacionRepository operacionRepository;
    private final ZoneId zonaHoraria;

    public LeadService(LeadRepository leadRepository,
                       AgenteRepository agenteRepository,
                       InteraccionRepository interaccionRepository,
                       OperacionRepository operacionRepository,
                       ZoneId zonaHoraria) {
        this.leadRepository = leadRepository;
        this.agenteRepository = agenteRepository;
        this.interaccionRepository = interaccionRepository;
        this.operacionRepository = operacionRepository;
        this.zonaHoraria = zonaHoraria;
    }


    // allEntries=true: el método recibe el email del agente, no su id; resolverlo
    // para un evict por key implicaría una query extra. Invalidar todo el cache
    // es seguro porque la entrada se rehidrata bajo demanda con TTL de 5 min.
    @CacheEvict(value = "estadisticasAgente", allEntries = true)
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

    public List<Interaccion> obtenerHistorialInteracciones(Long leadId) {

            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        return lead.getInteracciones();
    }



    @CacheEvict(value = "estadisticasAgente", allEntries = true)
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

        LocalDateTime fechaLimitePrioritarios = ahora.minusDays(7);
        List<Lead> prioritarios = leadRepository.findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead.CALIENTE, fechaLimitePrioritarios, email);

        // Incluye todos los seguimientos programados para hoy aunque la hora exacta
        // sea futura dentro del mismo día (ej: agendado 18:00, el usuario entra 09:00).
        LocalDateTime finDeHoy = inicioHoy.plusDays(1);
        List<Lead> seguimientos = leadRepository.findSeguimientosPendientes(
                finDeHoy, email, EstadoLead.INACTIVO
        );

        // Dedup: un lead que ya está en "prioritarios" no debe duplicarse en "seguimientos".
        // Prioritario manda porque marca un atraso > 7 días que requiere atención inmediata.
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

    @CacheEvict(value = "estadisticasAgente", allEntries = true)
    public Lead establecerLeadInactivo(Long id, String emailAgente) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar este lead.");
        }

         lead.setEstado(EstadoLead.INACTIVO);
         return leadRepository.save(lead);
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
        return leads.stream().map(this::toResumenDTO).toList();
    }

    public Page<LeadResumenDTO> obtenerResumenLeadsPorAgente(String email, Pageable pageable) {
        Pageable efectivo = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                 Sort.by(Sort.Direction.DESC, "fechaEntrada"));
        return leadRepository.findByAgenteEmailConInteraccionesPaginado(email, efectivo)
                .map(this::toResumenDTO);
    }

    private LeadResumenDTO toResumenDTO(Lead lead) {
        long ventas = operacionRepository.countByLeadIdAndTipo(lead.getId(), TipoOperacion.VENTA);
        long compras = operacionRepository.countByLeadIdAndTipo(lead.getId(), TipoOperacion.COMPRA);
        long interacciones = lead.getInteracciones().size();
        String ultimaInteraccion = lead.getInteracciones().isEmpty() ? null :
                lead.getInteracciones().get(lead.getInteracciones().size() - 1).getDetalle();
        return new LeadResumenDTO(
                lead.getId(), lead.getNombre(), lead.getApellido(),
                lead.getTelefono(), lead.getEmail(), lead.getEstado(),
                lead.getFechaEntrada(), lead.getUltimoContacto(),
                lead.getFechaProximoSeguimiento(),
                ventas, compras, interacciones, ultimaInteraccion
        );
    }

}
