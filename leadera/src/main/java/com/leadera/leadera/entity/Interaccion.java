package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leadera.leadera.enums.TipoInteraccion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "interaccion")
public class Interaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String detalle;
    private LocalDateTime fechaInteraccion;

    @Enumerated(EnumType.STRING)
    private TipoInteraccion tipoInteraccion;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "lead_id")
    private Lead lead;

    // Agente REAL que registró la interacción. Para un ASISTENTE es él mismo
    // (no su supervisor), así queda el registro de quién la hizo. Nullable para
    // las interacciones previas a la Fase 1 (se backfillean al dueño del lead).
    // @JsonIgnore: la entidad Interaccion se serializa directa en la respuesta de
    // crear; no queremos exponer el Agente (y su hash) en ese payload.
    // Exclusiones obligatorias: Interaccion usa @Data; sin ellas toString()/equals()
    // navegarían al Agente disparando queries y posibles ciclos.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Agente autor;

}
