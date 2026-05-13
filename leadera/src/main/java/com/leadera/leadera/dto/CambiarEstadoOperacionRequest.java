package com.leadera.leadera.dto;

import com.leadera.leadera.enums.EstadoOperacion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoOperacionRequest {

    @NotNull(message = "El estado no puede ser nulo")
    private EstadoOperacion estadoOperacion;
}
