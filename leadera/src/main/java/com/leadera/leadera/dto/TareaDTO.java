package com.leadera.leadera.dto;

import java.time.LocalDateTime;

// progreso: leads distintos contactados hoy por el asistente (0 si la tarea es discreta).
public record TareaDTO(
        Long id,
        String titulo,
        Long asignadoAId,
        String asignadoANombre,
        Long creadoPorId,
        Long leadId,
        Integer objetivo,
        int progreso,
        boolean completada,
        LocalDateTime fechaCompletada,
        LocalDateTime fechaObjetivo
) {
}
