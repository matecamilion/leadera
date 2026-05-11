package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.leadera.leadera.enums.TipoInteraccion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}
