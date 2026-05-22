package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CrearFotoRequest;
import com.leadera.leadera.dto.FotoPropiedadDTO;
import com.leadera.leadera.service.FotoPropiedadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/propiedades/{propiedadId}/fotos")
public class FotoPropiedadController {

    private final FotoPropiedadService fotoPropiedadService;

    public FotoPropiedadController(FotoPropiedadService fotoPropiedadService) {
        this.fotoPropiedadService = fotoPropiedadService;
    }

    @PostMapping
    public ResponseEntity<FotoPropiedadDTO> agregar(@PathVariable Long propiedadId,
                                                    @Valid @RequestBody CrearFotoRequest request,
                                                    Authentication authentication) {
        FotoPropiedadDTO creada = fotoPropiedadService.agregarFoto(propiedadId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<FotoPropiedadDTO>> listar(@PathVariable Long propiedadId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(fotoPropiedadService.listarFotos(propiedadId, authentication.getName()));
    }

    @DeleteMapping("/{fotoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long propiedadId,
                                         @PathVariable Long fotoId,
                                         Authentication authentication) {
        fotoPropiedadService.eliminarFoto(propiedadId, fotoId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
