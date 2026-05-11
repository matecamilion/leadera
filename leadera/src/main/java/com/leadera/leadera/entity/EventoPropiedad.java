package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.leadera.leadera.enums.TipoEvento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_propiedad")
@Data
@NoArgsConstructor
public class EventoPropiedad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipo;

    private LocalDateTime fecha;
    private String detalle;

    @ManyToOne
    @JoinColumn(name = "propiedad_id")
    @JsonBackReference
    private Propiedad propiedad;
}
