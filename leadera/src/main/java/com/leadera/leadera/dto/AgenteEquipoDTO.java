package com.leadera.leadera.dto;

import com.leadera.leadera.enums.RolAgente;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AgenteEquipoDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private RolAgente rol;
    private boolean activo;
    private long leadsActivos;
}
