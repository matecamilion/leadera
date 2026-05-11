package com.leadera.leadera.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "operacion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Operacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Enumerated(EnumType.STRING)
    private TipoOperacion tipoOperacion;

    @Enumerated(EnumType.STRING)
    private EstadoOperacion estadoOperacion;

    private String descripcion;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaCierre;

    private LocalDateTime fechaProximoSeguimiento;

    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = false)
    @JsonIgnore
    private Lead lead;

    @ManyToOne
    @JoinColumn(name = "agente_id", nullable = false)
    @JsonIgnore
    private Agente agente;

    @ManyToOne
    @JoinColumn(name = "propiedad_id")
    @JsonIgnoreProperties({"lead", "eventos"})
    private Propiedad propiedad;

    @ManyToOne
    @JoinColumn(name = "busqueda_id")
    private Busqueda busqueda;

}
