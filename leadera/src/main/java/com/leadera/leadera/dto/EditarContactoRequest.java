package com.leadera.leadera.dto;

public record EditarContactoRequest(
        String nombre,
        String apellido,
        String telefono,
        String email
) {
}
