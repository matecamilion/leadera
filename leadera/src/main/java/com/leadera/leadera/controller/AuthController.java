package com.leadera.leadera.controller;

import java.util.Map;
import com.leadera.leadera.model.Agente;
import com.leadera.leadera.model.LoginResponse;
import com.leadera.leadera.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Agente agente) {
        try {
            String respuesta = authService.registrar(agente);

            return ResponseEntity.ok(Map.of(
                    "mensaje", respuesta
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error al registrar: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Agente agente) {
        try {
            LoginResponse response = authService.login(agente.getEmail(), agente.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }
}
