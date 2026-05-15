package com.leadera.leadera.controller;

import java.util.Map;
import com.leadera.leadera.dto.LoginRequest;
import com.leadera.leadera.dto.LoginResponse;
import com.leadera.leadera.dto.RegisterRequest;
import com.leadera.leadera.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegisterRequest request) {
        String respuesta = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensaje", respuesta));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
}
