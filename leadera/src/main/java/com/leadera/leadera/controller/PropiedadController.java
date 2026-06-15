package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CompradorPotencialDTO;
import com.leadera.leadera.dto.CrearPropiedadRequest;
import com.leadera.leadera.dto.MatchDelDiaDTO;
import com.leadera.leadera.dto.EditarPropiedadRequest;
import com.leadera.leadera.dto.EventoOperacionResumenDTO;
import com.leadera.leadera.dto.PropiedadDTO;
import com.leadera.leadera.dto.PropiedadResumenDTO;
import com.leadera.leadera.service.PropiedadService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/propiedades")
public class PropiedadController {

    private final PropiedadService propiedadService;

    public PropiedadController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    @GetMapping
    public ResponseEntity<List<PropiedadResumenDTO>> listarMisPropiedades(Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerPropiedadesDelAgente(authentication.getName()));
    }

    @PostMapping("/lead/{leadId}")
    public ResponseEntity<PropiedadDTO> agregarPropiedad(@PathVariable Long leadId,
                                                         @Valid @RequestBody CrearPropiedadRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(propiedadService.agregarPropiedad(leadId, request, authentication.getName()));
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<PropiedadDTO>> listarPropiedades(@PathVariable Long leadId, Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerPropiedadesDeLead(leadId, authentication.getName()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PropiedadDTO> actualizarEstado(@PathVariable Long id,
                                                         @RequestBody Map<String, String> body,
                                                         Authentication authentication) {
        String estado = body.get("estado");
        return ResponseEntity.ok(propiedadService.actualizarEstado(id, estado, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropiedadDTO> obtenerPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerPorId(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropiedadDTO> editar(@PathVariable Long id,
                                               @RequestBody EditarPropiedadRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(propiedadService.editarPropiedad(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/eventos")
    public ResponseEntity<List<EventoOperacionResumenDTO>> listarEventos(@PathVariable Long id,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerEventosDePropiedad(id, authentication.getName()));
    }

    @GetMapping("/{id}/compradores-potenciales")
    public ResponseEntity<List<CompradorPotencialDTO>> compradoresPotenciales(@PathVariable Long id,
                                                                              Authentication authentication) {
        return ResponseEntity.ok(propiedadService.buscarCompradoresPotenciales(id, authentication.getName()));
    }

    @GetMapping("/mis-matches")
    public ResponseEntity<List<MatchDelDiaDTO>> obtenerMisMatches(Authentication authentication) {
        return ResponseEntity.ok(
                propiedadService.obtenerMatchesDelAgente(authentication.getName()));
    }

}
