package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoInteraccion;
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
public class CrearInteraccionRequest {

    @NotNull
    private TipoInteraccion tipoInteraccion;

    @NotBlank
    @Size(min = 3, max = 1000)
    private String detalle;

    private LocalDateTime fechaInteraccion;

    private LocalDateTime proximoContacto;
}
