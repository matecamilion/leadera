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
}
