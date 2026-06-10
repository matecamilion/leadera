package com.leadera.leadera.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private boolean debeCambiarPassword;

    public LoginResponse(String token, String nombre, String apellido, String email,
                         String rol, boolean debeCambiarPassword) {
        this.token = token;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
        this.debeCambiarPassword = debeCambiarPassword;
    }
}
