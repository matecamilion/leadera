package com.leadera.leadera.service;

import com.leadera.leadera.dto.AsistenteResumenDTO;
import com.leadera.leadera.dto.CrearTareaRequest;
import com.leadera.leadera.dto.TareaDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Tarea;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.TareaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final InteraccionRepository interaccionRepository;
    private final AgenteRepository agenteRepository;
    private final LeadRepository leadRepository;
    private final AgenteContextResolver agenteContextResolver;
    private final ZoneId zonaHoraria;

    public TareaService(TareaRepository tareaRepository,
                        InteraccionRepository interaccionRepository,
                        AgenteRepository agenteRepository,
                        LeadRepository leadRepository,
                        AgenteContextResolver agenteContextResolver,
                        ZoneId zonaHoraria) {
        this.tareaRepository = tareaRepository;
        this.interaccionRepository = interaccionRepository;
        this.agenteRepository = agenteRepository;
        this.leadRepository = leadRepository;
        this.agenteContextResolver = agenteContextResolver;
        this.zonaHoraria = zonaHoraria;
    }

    // 3a — El AGENTE/DUENO crea una tarea para uno de SUS asistentes.
    // Un ASISTENTE también puede crear, pero solo autoasignándosela.
    @Transactional
    public TareaDTO crearTarea(CrearTareaRequest request, String emailCreador) {
        Agente actor = agenteContextResolver.resolverActor(emailCreador);

        // Un asistente solo se autoasigna tareas (no puede crearle a nadie más).
        if (actor.getRol() == RolAgente.ASISTENTE
                && !actor.getId().equals(request.getAsignadoAId())) {
            throw new UnauthorizedActionException("Un asistente solo puede asignarse tareas a sí mismo.");
        }

        Agente asignadoA = agenteRepository.findById(request.getAsignadoAId())
                .orElseThrow(() -> new ResourceNotFoundException("Agente asignado no encontrado"));

        // Cross-tenant: el asignado debe ser de la misma inmobiliaria que el creador.
        // (Para el asistente autoasignándose es trivialmente cierto.)
        if (!asignadoA.getInmobiliaria().getId().equals(actor.getInmobiliaria().getId())) {
            throw new UnauthorizedActionException("No podés asignar tareas a agentes de otra inmobiliaria.");
        }

        // Jerarquía (solo para AGENTE/DUENO): se asigna únicamente a los propios
        // asistentes (supervisor == actor). No aplica a la autoasignación del
        // asistente: su supervisor es su agente, no él mismo.
        if (actor.getRol() != RolAgente.ASISTENTE
                && (asignadoA.getSupervisor() == null
                        || !asignadoA.getSupervisor().getId().equals(actor.getId()))) {
            throw new UnauthorizedActionException("Solo podés asignar tareas a tus propios asistentes.");
        }

        Tarea tarea = new Tarea();
        tarea.setTitulo(request.getTitulo());
        tarea.setAsignadoA(asignadoA);
        tarea.setCreadoPor(actor);
        tarea.setObjetivo(request.getObjetivo());
        tarea.setFechaObjetivo(request.getFechaObjetivo());
        tarea.setInmobiliaria(actor.getInmobiliaria());
        tarea.setFechaCreacion(LocalDateTime.now(zonaHoraria));

        if (request.getLeadId() != null) {
            Lead lead = leadRepository.findById(request.getLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));
            // El lead debe pertenecer al PROPIETARIO EFECTIVO del creador (regla de
            // Fase 1): para AGENTE/DUENO es él mismo; para un asistente que se
            // autoasigna, los leads que trabaja son los de su supervisor. Cierra el
            // cruce de inmobiliaria de forma implícita.
            Agente propietario = agenteContextResolver.resolverPropietario(actor);
            if (!lead.getAgente().getId().equals(propietario.getId())) {
                throw new UnauthorizedActionException("No podés asociar la tarea a un lead que no es tuyo.");
            }
            tarea.setLead(lead);
        }

        Tarea guardada = tareaRepository.save(tarea);
        return toDTO(guardada, calcularProgreso(guardada, asignadoA.getId()));
    }

    // 3b — El asistente (o cualquier actor) ve SUS tareas de hoy; las cuotas se
    // auto-completan si alcanzaron el objetivo.
    @Transactional
    public List<TareaDTO> obtenerTareasDelDia(String emailActor) {
        Agente actor = agenteContextResolver.resolverActor(emailActor);

        LocalDateTime inicioHoy = LocalDateTime.now(zonaHoraria).toLocalDate().atStartOfDay();
        LocalDateTime finHoyIncl = inicioHoy.plusDays(1).minusNanos(1);

        List<Tarea> tareas = tareaRepository.findByAsignadoAIdAndFechaObjetivoBetween(
                actor.getId(), inicioHoy, finHoyIncl);

        List<TareaDTO> salida = new ArrayList<>(tareas.size());
        for (Tarea tarea : tareas) {
            int progreso = calcularProgreso(tarea, actor.getId());

            // Auto-completado de cuotas: si llegó al objetivo y aún no estaba marcada.
            if (tarea.getObjetivo() != null && !tarea.isCompletada() && progreso >= tarea.getObjetivo()) {
                tarea.setCompletada(true);
                tarea.setFechaCompletada(LocalDateTime.now(zonaHoraria));
                tareaRepository.save(tarea);
            }

            salida.add(toDTO(tarea, progreso));
        }
        return salida;
    }

    // 3c — El asistente marca a mano una tarea DISCRETA propia.
    @Transactional
    public TareaDTO completarTarea(Long tareaId, String emailActor) {
        Agente actor = agenteContextResolver.resolverActor(emailActor);

        // Cross-tenant: la tarea debe ser de la inmobiliaria del actor.
        Tarea tarea = tareaRepository.findByIdAndInmobiliariaId(tareaId, actor.getInmobiliaria().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));

        // Solo el asignado puede completarla.
        if (!tarea.getAsignadoA().getId().equals(actor.getId())) {
            throw new UnauthorizedActionException("No tenés permiso para completar esta tarea.");
        }

        // Las cuotas se completan solas; no se marcan a mano.
        if (tarea.getObjetivo() != null) {
            throw new UnauthorizedActionException("Esta tarea se completa automáticamente.");
        }

        tarea.setCompletada(true);
        tarea.setFechaCompletada(LocalDateTime.now(zonaHoraria));
        Tarea guardada = tareaRepository.save(tarea);

        return toDTO(guardada, 0);
    }

    // 3d — El supervisor ve las tareas que le creó a un asistente suyo.
    @Transactional(readOnly = true)
    public List<TareaDTO> obtenerTareasDeAsistente(Long asistenteId, String emailSupervisor) {
        Agente supervisor = agenteContextResolver.resolverActor(emailSupervisor);

        Agente asistente = agenteRepository.findById(asistenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Asistente no encontrado"));

        // Cross-tenant + jerarquía: mismo tenant y supervisor == el que pide.
        boolean mismaInmobiliaria = asistente.getInmobiliaria().getId()
                .equals(supervisor.getInmobiliaria().getId());
        boolean esSuAsistente = asistente.getSupervisor() != null
                && asistente.getSupervisor().getId().equals(supervisor.getId());
        if (!mismaInmobiliaria || !esSuAsistente) {
            throw new UnauthorizedActionException("Este asistente no está bajo tu supervisión.");
        }

        List<Tarea> tareas = tareaRepository.findByCreadoPorIdAndAsignadoAId(supervisor.getId(), asistenteId);

        List<TareaDTO> salida = new ArrayList<>(tareas.size());
        for (Tarea tarea : tareas) {
            // Solo se calcula progreso; acá NO se auto-completa (vista de lectura del supervisor).
            salida.add(toDTO(tarea, calcularProgreso(tarea, asistenteId)));
        }
        return salida;
    }

    // Asistentes del actor (para el selector del frontend). Lista vacía si no tiene.
    @Transactional(readOnly = true)
    public List<AsistenteResumenDTO> obtenerMisAsistentes(String emailActor) {
        Agente actor = agenteContextResolver.resolverActor(emailActor);
        return agenteRepository.findBySupervisorId(actor.getId()).stream()
                .map(a -> new AsistenteResumenDTO(a.getId(), a.getNombre(), a.getApellido()))
                .toList();
    }

    // ---- helpers ----

    // Progreso de una cuota: leads distintos contactados HOY por el asistente (autor).
    // Para tareas discretas (objetivo == null) el progreso no aplica: devuelve 0.
    private int calcularProgreso(Tarea tarea, Long autorId) {
        if (tarea.getObjetivo() == null) {
            return 0;
        }
        LocalDateTime inicioHoy = LocalDateTime.now(zonaHoraria).toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        return (int) interaccionRepository.countLeadsDistinctPorAutorEnRango(autorId, inicioHoy, finHoy);
    }

    private TareaDTO toDTO(Tarea tarea, int progreso) {
        Agente asignadoA = tarea.getAsignadoA();
        String nombre = ((asignadoA.getNombre() != null ? asignadoA.getNombre() : "") + " "
                + (asignadoA.getApellido() != null ? asignadoA.getApellido() : "")).trim();
        return new TareaDTO(
                tarea.getId(),
                tarea.getTitulo(),
                asignadoA.getId(),
                nombre,
                tarea.getCreadoPor().getId(),
                tarea.getLead() != null ? tarea.getLead().getId() : null,
                tarea.getObjetivo(),
                progreso,
                tarea.isCompletada(),
                tarea.getFechaCompletada(),
                tarea.getFechaObjetivo()
        );
    }
}
