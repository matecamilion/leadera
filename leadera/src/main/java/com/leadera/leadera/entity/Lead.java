package com.leadera.leadera.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.leadera.leadera.enums.EstadoLead;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leads")
@Data
@NoArgsConstructor
public class Lead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String telefono;

    @Column(unique = true)
    private String email;
    private LocalDateTime fechaEntrada;
    private LocalDateTime ultimoContacto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoLead estado;

    private LocalDateTime fechaProximoSeguimiento;

    @ManyToOne
    @JoinColumn(name = "agente_id")
    @JsonIgnoreProperties({"leads", "password", "hibernateLazyInitializer", "handler"})
    private Agente agente;

    @JsonManagedReference
    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL)
    private List<Interaccion> interacciones = new ArrayList<>();


    @JsonManagedReference
    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL)
    private List<Propiedad> propiedades = new ArrayList<>();


}
