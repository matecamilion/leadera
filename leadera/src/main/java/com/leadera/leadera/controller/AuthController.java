package com.leadera.leadera.controller;

import java.util.Map;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.dto.LoginResponse;
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
    public ResponseEntity<Map<String, String>> registrar(@RequestBody Agente agente) {
        String respuesta = authService.registrar(agente);
        return ResponseEntity.ok(Map.of("mensaje", respuesta));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Agente agente) {
        LoginResponse response = authService.login(agente.getEmail(), agente.getPassword());
        return ResponseEntity.ok(response);
    }
}
