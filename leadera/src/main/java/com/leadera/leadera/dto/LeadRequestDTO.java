package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeadRequestDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String telefono;

    @Email
    private String email;

    @NotNull
    private EstadoLead estado;

    private LocalDateTime fechaProximoSeguimiento;

    @Size(max = 100)
    private String origen;

    @Size(max = 500)
    private String descripcionInicial;
}
