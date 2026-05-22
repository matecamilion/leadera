package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import java.util.ArrayList;
import java.util.List;

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

    // Las fotos se serializan dentro de Propiedad en el GET de detalle.
    // JsonManagedReference evita el loop infinito con el JsonBackReference de FotoPropiedad.
    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @JsonManagedReference("propiedad-fotos")
    private List<FotoPropiedad> fotos = new ArrayList<>();

    @Transient
    public long getDiasEnMercado() {
        if (fechaPublicacion == null) return 0;
        return ChronoUnit.DAYS.between(fechaPublicacion.toLocalDate(), LocalDate.now());
    }

}
