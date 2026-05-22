package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "foto_propiedad")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FotoPropiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propiedad_id", nullable = false)
    @JsonBackReference("propiedad-fotos")
    private Propiedad propiedad;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;
}
