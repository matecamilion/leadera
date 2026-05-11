package com.leadera.leadera.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name="busqueda")
@NoArgsConstructor
@AllArgsConstructor
@Data

public class Busqueda {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer cantidadAmbientes;
    private Integer metrosTotales;
    private Integer metrosCubiertos;
    private Integer metrosDescubiertos;
    private TipoVivienda tipoVivienda;
    private String zona;
    private String observaciones;


}
