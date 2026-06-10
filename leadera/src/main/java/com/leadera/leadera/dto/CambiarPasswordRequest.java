package com.leadera.leadera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CambiarPasswordRequest {

    @NotBlank
    private String passwordActual;

    @NotBlank
    @Size(min = 6, max = 100)
    private String passwordNueva;
}
