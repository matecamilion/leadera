package com.leadera.leadera.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String email;

    // Constructor, Getters y Setters
    public LoginResponse(String token, String nombre, String apellido, String email) {
        this.token = token;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
    }
}
