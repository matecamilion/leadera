package com.leadera.leadera.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActualizarPerfilRequest {

    @NotBlank
    @Size(min = 1, max = 80)
    private String nombre;

    @NotBlank
    @Size(min = 1, max = 80)
    private String apellido;

    @NotNull
    @Min(1)
    private Integer metaMensualCierres;
}
