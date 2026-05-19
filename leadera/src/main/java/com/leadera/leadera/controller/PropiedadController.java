package com.leadera.leadera.controller;

import com.leadera.leadera.dto.EditarPropiedadRequest;
import com.leadera.leadera.dto.EventoOperacionResumenDTO;
import com.leadera.leadera.dto.PropiedadResumenDTO;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.service.PropiedadService;
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
    public ResponseEntity<Propiedad> agregarPropiedad(@PathVariable Long leadId,
                                                      @RequestBody Propiedad propiedad,
                                                      Authentication authentication) {
        return ResponseEntity.ok(propiedadService.agregarPropiedad(leadId, propiedad, authentication.getName()));
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<Propiedad>> listarPropiedades(@PathVariable Long leadId, Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerPropiedadesDeLead(leadId, authentication.getName()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Propiedad> actualizarEstado(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body,
                                                      Authentication authentication) {
        String estado = body.get("estado");
        return ResponseEntity.ok(propiedadService.actualizarEstado(id, estado, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Propiedad> obtenerPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerPorId(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Propiedad> editar(@PathVariable Long id,
                                            @RequestBody EditarPropiedadRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(propiedadService.editarPropiedad(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/eventos")
    public ResponseEntity<List<EventoOperacionResumenDTO>> listarEventos(@PathVariable Long id,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(propiedadService.obtenerEventosDePropiedad(id, authentication.getName()));
    }

}
