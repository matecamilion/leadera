package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.leadera.leadera.enums.TipoEvento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_operacion")
@Data
@NoArgsConstructor
public class EventoOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipo;

    private LocalDateTime fecha;
    private String detalle;

    @ManyToOne
    @JoinColumn(name = "operacion_id")
    @JsonBackReference
    private Operacion operacion;
}
