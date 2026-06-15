package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    // Tareas del asistente para una fecha objetivo dada (vista del asistente).
    List<Tarea> findByAsignadoAIdAndFechaObjetivoBetween(Long id, LocalDateTime desde, LocalDateTime hasta);

    // Tareas creadas por el agente sobre un asistente (vista del supervisor).
    List<Tarea> findByCreadoPorIdAndAsignadoAId(Long creadoPorId, Long asignadoAId);

    // Tareas de la inmobiliaria (para validación cross-tenant al operar por id).
    Optional<Tarea> findByIdAndInmobiliariaId(Long id, Long inmobiliariaId);

    // Tareas completadas hoy por un asistente (métrica de la vista "Mi equipo" del agente).
    @Query("SELECT COUNT(t) FROM Tarea t WHERE t.asignadoA.id = :asistenteId " +
            "AND t.completada = true " +
            "AND t.fechaCompletada >= :inicioHoy")
    long countTareasCompletadasHoy(@Param("asistenteId") Long asistenteId,
                                   @Param("inicioHoy") LocalDateTime inicioHoy);

    // Tareas de hoy + arrastre de hasta 3 días: las vencidas solo si no están
    // completadas; las de hoy independientemente del estado (para mostrarlas tachadas).
    @Query("""
            SELECT t FROM Tarea t
            WHERE t.asignadoA.id = :agenteId
            AND (
                (t.fechaObjetivo >= :inicioVentana AND t.fechaObjetivo <= :finHoy)
                OR
                (t.fechaObjetivo >= :inicioVentana AND t.fechaObjetivo < :inicioHoy AND t.completada = false)
            )
            ORDER BY t.completada ASC, t.fechaObjetivo ASC
            """)
    List<Tarea> findTareasActivasConArrastre(
            @Param("agenteId") Long agenteId,
            @Param("inicioVentana") LocalDateTime inicioVentana,
            @Param("inicioHoy") LocalDateTime inicioHoy,
            @Param("finHoy") LocalDateTime finHoy
    );

    // Historial de los últimos N días: todas las tareas ordenadas por fechaObjetivo DESC.
    @Query("""
            SELECT t FROM Tarea t
            WHERE t.asignadoA.id = :agenteId
            AND t.fechaObjetivo >= :desde
            AND t.fechaObjetivo < :hasta
            ORDER BY t.fechaObjetivo DESC
            """)
    List<Tarea> findHistorialByAgenteId(
            @Param("agenteId") Long agenteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    // Métricas mensuales para la card de cumplimiento.
    @Query("""
            SELECT COUNT(t) FROM Tarea t
            WHERE t.asignadoA.id = :agenteId
            AND t.completada = true
            AND t.fechaCompletada >= :inicioMes
            AND t.fechaCompletada < :inicioMesSiguiente
            """)
    long countTareasCompletadasMes(
            @Param("agenteId") Long agenteId,
            @Param("inicioMes") LocalDateTime inicioMes,
            @Param("inicioMesSiguiente") LocalDateTime inicioMesSiguiente
    );

    @Query("""
            SELECT COUNT(t) FROM Tarea t
            WHERE t.asignadoA.id = :agenteId
            AND t.fechaObjetivo >= :inicioMes
            AND t.fechaObjetivo < :inicioMesSiguiente
            """)
    long countTareasTotalesMes(
            @Param("agenteId") Long agenteId,
            @Param("inicioMes") LocalDateTime inicioMes,
            @Param("inicioMesSiguiente") LocalDateTime inicioMesSiguiente
    );
}
