package com.leadera.leadera.dto;

import jakarta.validation.constraints.Min;
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
public class CrearFotoRequest {

    @NotBlank
    @Size(max = 2048)
    private String url;

    @Min(0)
    private Integer orden = 0;
}
