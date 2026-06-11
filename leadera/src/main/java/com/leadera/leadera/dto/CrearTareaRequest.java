package com.leadera.leadera.dto;

import jakarta.validation.constraints.Min;
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
public class CrearTareaRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotNull
    private Long asignadoAId;

    // Nullable: la tarea puede no estar atada a un lead específico.
    private Long leadId;

    // Nullable: null = tarea discreta (manual). Con valor (>=1) = cuota automática.
    @Min(1)
    private Integer objetivo;

    @NotNull
    private LocalDateTime fechaObjetivo;
}
