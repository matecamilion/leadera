package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CambiarEstadoOperacionRequest;
import com.leadera.leadera.dto.OperacionPipelineDTO;
import com.leadera.leadera.service.OperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/operaciones")
public class OperacionPipelineController {

    private final OperacionService operacionService;

    public OperacionPipelineController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @GetMapping
    public ResponseEntity<List<OperacionPipelineDTO>> obtenerPipeline(Authentication authentication) {
        return ResponseEntity.ok(
                operacionService.obtenerPipelineDelAgente(authentication.getName())
        );
    }

    @GetMapping("/cerradas")
    public ResponseEntity<List<OperacionPipelineDTO>> obtenerCerradas(Authentication authentication) {
        return ResponseEntity.ok(
                operacionService.obtenerOperacionesCerradasDelAgente(authentication.getName())
        );
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OperacionPipelineDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoOperacionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.cambiarEstadoOperacionPipeline(
                        id,
                        request.getEstadoOperacion(),
                        authentication.getName()
                )
        );
    }
}
