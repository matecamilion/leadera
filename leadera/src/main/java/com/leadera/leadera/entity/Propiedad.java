package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.leadera.leadera.enums.EstadoPropiedad;
import com.leadera.leadera.enums.TipoVivienda;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "propiedad")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Propiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String direccion;
    private BigDecimal precio;
    private Integer cantidadAmbientes;
    private Integer metrosTotales;
    private Integer metrosCubiertos;

    @Enumerated(EnumType.STRING)
    private TipoVivienda tipoVivienda;

    private String zona;
    private String observaciones;
    private LocalDateTime fechaPublicacion;

    @ManyToOne
    @JoinColumn(name = "lead_id")
    @JsonBackReference
    private Lead lead;

    @Enumerated(EnumType.STRING)
    private EstadoPropiedad estado = EstadoPropiedad.DISPONIBLE;

    @Transient
    public long getDiasEnMercado() {
        if (fechaPublicacion == null) return 0;
        return ChronoUnit.DAYS.between(fechaPublicacion.toLocalDate(), LocalDate.now());
    }

}
