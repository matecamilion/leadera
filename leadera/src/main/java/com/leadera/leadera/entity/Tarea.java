package com.leadera.leadera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Tarea que un AGENTE/DUENO asigna a su ASISTENTE. Dos sabores:
//   - discreta: objetivo == null, se completa a mano (completarTarea).
//   - cuota:    objetivo != null, se auto-completa contando LEADS DISTINTOS
//               contactados por el asistente (autor de la interacción) en el día.
// No usa @Data: hay varios @ManyToOne (FKs potencialmente circulares vía Agente),
// así que generamos solo getters/setters y excluimos las relaciones de
// toString()/equals()/hashCode() para evitar recursión y queries lazy no deseadas.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_a_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Agente asignadoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Agente creadoPor;

    // Nullable: una tarea puede no estar atada a un lead específico.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmobiliaria_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Inmobiliaria inmobiliaria;

    // null = tarea discreta (manual); con número = cuota automática (meta de leads).
    @Column(name = "objetivo")
    private Integer objetivo;

    @Column(name = "completada", nullable = false)
    private boolean completada = false;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    @Column(name = "fecha_objetivo", nullable = false)
    private LocalDateTime fechaObjetivo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
