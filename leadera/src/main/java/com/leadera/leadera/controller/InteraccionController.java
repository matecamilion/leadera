package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CrearInteraccionRequest;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.service.InteraccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leads/{leadId}/interacciones")
public class InteraccionController {
    private final InteraccionService interaccionService;

    public InteraccionController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    @PostMapping
    public ResponseEntity<Interaccion> crear(@PathVariable Long leadId,
                                             @Valid @RequestBody CrearInteraccionRequest request,
                                             Authentication authentication) {
        Interaccion creada = interaccionService.crearInteraccion(leadId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

}
