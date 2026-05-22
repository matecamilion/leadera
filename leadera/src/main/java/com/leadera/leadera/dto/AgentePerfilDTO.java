package com.leadera.leadera.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgentePerfilDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Integer metaMensualCierres;
}
