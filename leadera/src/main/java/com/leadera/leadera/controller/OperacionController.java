package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CrearEventoRequest;
import com.leadera.leadera.dto.EventoOperacionDTO;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.service.OperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leads/{leadId}/operaciones")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping
    public ResponseEntity<Operacion> crearOperacion(
            @PathVariable Long leadId,
            @RequestBody Operacion operacion,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.crearOperacion(
                        leadId,
                        operacion,
                        authentication.getName()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<Operacion>> obtenerOperacionesDelLead(
            @PathVariable Long leadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.obtenerOperacionesDelLead(
                        leadId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/abiertas")
    public ResponseEntity<List<Operacion>> obtenerOperacionesAbiertasDelLead(
            @PathVariable Long leadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.obtenerOperacionesAbiertasDelLead(
                        leadId,
                        authentication.getName()
                )
        );
    }


    @GetMapping("/{operacionId}")
    public ResponseEntity<Operacion> obtenerOperacionPorId(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.obtenerOperacionPorId(
                        leadId,
                        operacionId,
                        authentication.getName()
                )
        );
    }


    @PatchMapping("/{operacionId}/estado")
    public ResponseEntity<Operacion> cambiarEstadoOperacion(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            @RequestParam EstadoOperacion estadoOperacion,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.cambiarEstadoOperacion(
                        leadId,
                        operacionId,
                        estadoOperacion,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/{operacionId}/eventos")
    public ResponseEntity<EventoOperacionDTO> registrarEvento(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            @Valid @RequestBody CrearEventoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.registrarEvento(leadId, operacionId, request, authentication.getName())
        );
    }

    @GetMapping("/{operacionId}/eventos")
    public ResponseEntity<List<EventoOperacionDTO>> listarEventos(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.obtenerEventos(leadId, operacionId, authentication.getName())
        );
    }
}