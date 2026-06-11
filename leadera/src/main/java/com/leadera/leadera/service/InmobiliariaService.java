package com.leadera.leadera.service;

import com.leadera.leadera.dto.AgenteDashboardDTO;
import com.leadera.leadera.dto.AgenteEquipoDTO;
import com.leadera.leadera.dto.AsistenteStatsDTO;
import com.leadera.leadera.dto.CrearAgenteEquipoRequest;
import com.leadera.leadera.dto.EquipoStatsDTO;
import com.leadera.leadera.dto.LeadEquipoDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gestión del equipo de una inmobiliaria. Los métodos del dueño reciben al
// dueño ya autenticado (el controller lo resuelve vía AgenteAutenticadoService
// y el @PreAuthorize garantiza el rol DUENO). Los métodos de asistentes
// (crearAsistente / listarAsistentes / cambiarActivoAsistente) reciben al
// SUPERVISOR autenticado y validan por relación de supervisión, no por rol.
@Service
@RequiredArgsConstructor
public class InmobiliariaService {

    private final AgenteRepository agenteRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final TareaRepository tareaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ZoneId zonaHoraria;

    // Alta de agente del equipo con password temporal: debe cambiarla en su
    // primer login (debeCambiarPassword = true).
    public AgenteEquipoDTO crearAgente(CrearAgenteEquipoRequest request, Agente dueno) {
        if (agenteRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }

        Agente agente = new Agente();
        agente.setNombre(request.getNombre());
        agente.setApellido(request.getApellido());
        agente.setEmail(request.getEmail());
        agente.setPassword(passwordEncoder.encode(request.getPasswordTemporal()));
        agente.setInmobiliaria(dueno.getInmobiliaria());
        agente.setRol(RolAgente.AGENTE);
        agente.setDebeCambiarPassword(true);
        agente.setActivo(true);

        try {
            agente = agenteRepository.save(agente);
        } catch (DataIntegrityViolationException ex) {
            // Carrera contra el unique de agente.email en DB.
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }
        return toEquipoDTO(agente, 0L);
    }

    public List<AgenteEquipoDTO> listarEquipo(Agente dueno) {
        Long inmobiliariaId = dueno.getInmobiliaria().getId();
        Map<Long, Long> leadsActivosPorAgente = contarLeadsActivos(inmobiliariaId);

        return agenteRepository.findByInmobiliariaIdOrderByIdAsc(inmobiliariaId).stream()
                .map(a -> toEquipoDTO(a, leadsActivosPorAgente.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    public AgenteEquipoDTO cambiarActivo(Long agenteId, boolean activo, Agente dueno) {
        Agente agente = agenteRepository.findById(agenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        // 404 y no 403: no revelamos a un dueño la existencia de agentes
        // de otras inmobiliarias.
        if (!agente.getInmobiliaria().getId().equals(dueno.getInmobiliaria().getId())) {
            throw new ResourceNotFoundException("Agente no encontrado");
        }
        if (agente.getId().equals(dueno.getId())) {
            throw new BadRequestException("No podés desactivar tu propia cuenta");
        }

        agente.setActivo(activo);
        agente = agenteRepository.save(agente);

        Map<Long, Long> leadsActivos = contarLeadsActivos(dueno.getInmobiliaria().getId());
        return toEquipoDTO(agente, leadsActivos.getOrDefault(agente.getId(), 0L));
    }

    // ---------- Asistentes de un AGENTE (jerarquía DUENO -> AGENTE -> ASISTENTE) ----------

    // Alta de asistente: igual que crearAgente pero con rol ASISTENTE y supervisor
    // seteado. Hereda la inmobiliaria del supervisor (invariante cross-tenant de Fase 0).
    public AgenteEquipoDTO crearAsistente(CrearAgenteEquipoRequest request, Agente supervisor) {
        if (agenteRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }

        Agente asistente = new Agente();
        asistente.setNombre(request.getNombre());
        asistente.setApellido(request.getApellido());
        asistente.setEmail(request.getEmail());
        asistente.setPassword(passwordEncoder.encode(request.getPasswordTemporal()));
        asistente.setInmobiliaria(supervisor.getInmobiliaria());
        asistente.setRol(RolAgente.ASISTENTE);
        asistente.setSupervisor(supervisor);
        asistente.setDebeCambiarPassword(true);
        asistente.setActivo(true);

        try {
            asistente = agenteRepository.save(asistente);
        } catch (DataIntegrityViolationException ex) {
            // Carrera contra el unique de agente.email en DB.
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }
        return toEquipoDTO(asistente, 0L);
    }

    // Asistentes del supervisor con sus métricas del día. leadsActivos es el total
    // del SUPERVISOR (los asistentes trabajan sobre los leads de su agente, no
    // tienen leads propios).
    public List<AsistenteStatsDTO> listarAsistentes(Agente supervisor) {
        long leadsActivosDelSupervisor = leadRepository
                .countByAgenteIdAndEstadoNot(supervisor.getId(), EstadoLead.INACTIVO);
        LocalDateTime inicioHoy = LocalDateTime.now(zonaHoraria).toLocalDate().atStartOfDay();

        return agenteRepository.findBySupervisorId(supervisor.getId()).stream()
                .map(a -> toAsistenteStatsDTO(a, leadsActivosDelSupervisor,
                        tareaRepository.countTareasCompletadasHoy(a.getId(), inicioHoy)))
                .toList();
    }

    // Activa/desactiva un asistente. Mismo patrón que cambiarActivo del dueño,
    // pero la autoridad es la relación de supervisión (no el rol DUENO).
    public AsistenteStatsDTO cambiarActivoAsistente(Long asistenteId, boolean activo, Agente supervisor) {
        Agente asistente = agenteRepository.findById(asistenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Asistente no encontrado"));

        // 404 y no 403: no revelamos la existencia de agentes que no supervisa
        // (cubre también el cross-tenant: un asistente de otra inmobiliaria
        // jamás tiene a este actor como supervisor).
        if (asistente.getSupervisor() == null
                || !asistente.getSupervisor().getId().equals(supervisor.getId())) {
            throw new ResourceNotFoundException("Asistente no encontrado");
        }

        asistente.setActivo(activo);
        asistente = agenteRepository.save(asistente);

        long leadsActivosDelSupervisor = leadRepository
                .countByAgenteIdAndEstadoNot(supervisor.getId(), EstadoLead.INACTIVO);
        LocalDateTime inicioHoy = LocalDateTime.now(zonaHoraria).toLocalDate().atStartOfDay();
        return toAsistenteStatsDTO(asistente, leadsActivosDelSupervisor,
                tareaRepository.countTareasCompletadasHoy(asistente.getId(), inicioHoy));
    }

    private AsistenteStatsDTO toAsistenteStatsDTO(Agente asistente, long leadsActivos,
                                                  long tareasCompletadasHoy) {
        return new AsistenteStatsDTO(
                asistente.getId(), asistente.getNombre(), asistente.getApellido(),
                asistente.getEmail(), asistente.isActivo(), leadsActivos, tareasCompletadasHoy);
    }

    public Page<LeadEquipoDTO> obtenerLeadsDelEquipo(Agente dueno, Pageable pageable) {
        return leadService.obtenerResumenLeadsPorInmobiliaria(dueno.getInmobiliaria().getId(), pageable);
    }

    // Estadísticas agregadas del equipo. Reutiliza obtenerEstadisticasAgente
    // por cada agente activo: al invocarse a través del proxy de LeadService,
    // el @Cacheable (key = agenteId) sigue funcionando.
    public EquipoStatsDTO obtenerStats(Agente dueno) {
        List<Agente> activos = agenteRepository
                .findByInmobiliariaIdOrderByIdAsc(dueno.getInmobiliaria().getId()).stream()
                .filter(Agente::isActivo)
                .toList();

        List<EquipoStatsDTO.AgenteStatsDTO> porAgente = new ArrayList<>();
        long totalActivos = 0, totalCalientes = 0, totalTibios = 0, totalFrios = 0;
        long totalGanadosMes = 0, totalNuevosDelMes = 0, totalPerdidos = 0, totalInteracciones7d = 0;
        double sumaTiempoRespuesta = 0;
        int agentesConTiempo = 0;

        for (Agente agente : activos) {
            AgenteDashboardDTO stats = leadService.obtenerEstadisticasAgente(agente.getId());
            porAgente.add(new EquipoStatsDTO.AgenteStatsDTO(
                    agente.getId(), agente.getNombre(), agente.getApellido(), stats));

            totalActivos += stats.getActivos();
            totalCalientes += stats.getCalientes();
            totalTibios += stats.getTibios();
            totalFrios += stats.getFrios();
            totalGanadosMes += stats.getGanadosMes();
            totalNuevosDelMes += stats.getNuevosDelMes();
            totalPerdidos += stats.getPerdidos();
            totalInteracciones7d += stats.getInteracciones7d();
            if (stats.getTiempoRespuesta() > 0) {
                sumaTiempoRespuesta += stats.getTiempoRespuesta();
                agentesConTiempo++;
            }
        }

        // La tasa del equipo se recalcula sobre los totales (no se promedian
        // porcentajes); el tiempo de respuesta es promedio simple entre los
        // agentes que tienen dato.
        double tasaConversion = totalNuevosDelMes > 0
                ? Math.round((double) totalGanadosMes / totalNuevosDelMes * 100 * 10.0) / 10.0
                : 0.0;
        double tiempoRespuesta = agentesConTiempo > 0
                ? Math.round(sumaTiempoRespuesta / agentesConTiempo * 10.0) / 10.0
                : 0.0;

        AgenteDashboardDTO totales = new AgenteDashboardDTO(
                totalActivos, totalCalientes, totalTibios, totalFrios,
                totalGanadosMes, totalNuevosDelMes, totalPerdidos,
                totalInteracciones7d, tasaConversion, tiempoRespuesta);

        return new EquipoStatsDTO(totales, porAgente);
    }

    private Map<Long, Long> contarLeadsActivos(Long inmobiliariaId) {
        Map<Long, Long> resultado = new HashMap<>();
        for (Object[] fila : leadRepository.countLeadsActivosPorAgente(inmobiliariaId)) {
            resultado.put((Long) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    private AgenteEquipoDTO toEquipoDTO(Agente agente, long leadsActivos) {
        return new AgenteEquipoDTO(
                agente.getId(), agente.getNombre(), agente.getApellido(),
                agente.getEmail(), agente.getRol(), agente.isActivo(), leadsActivos);
    }
}
