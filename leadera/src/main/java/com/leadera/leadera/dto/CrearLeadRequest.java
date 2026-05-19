package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoLead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CrearLeadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    @Email(message = "El email debe tener formato válido")
    private String email;

    // Opcional: default FRIO en el service si viene null
    private EstadoLead estado;

    @Size(max = 100, message = "El origen no puede superar los 100 caracteres")
    private String origen;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcionInicial;

    private LocalDateTime fechaProximoSeguimiento;
}
