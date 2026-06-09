package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoInteraccion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InteraccionDTO {
    private Long id;
    private TipoInteraccion tipoInteraccion;
    private String detalle;
    private LocalDateTime fechaInteraccion;
}
