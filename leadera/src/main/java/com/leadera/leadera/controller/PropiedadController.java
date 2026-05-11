package com.leadera.leadera.controller;

import com.leadera.leadera.entity.EventoPropiedad;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.service.PropiedadService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/propiedades")
public class PropiedadController {

    private final PropiedadService propiedadService;

    public PropiedadController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }


    @PostMapping("/lead/{leadId}")
    public ResponseEntity<Propiedad> agregarPropiedad(@PathVariable Long leadId,
                                                      @RequestBody Propiedad propiedad,
                                                      Authentication authentication) {
        return ResponseEntity.ok(propiedadService.agregarPropiedad(leadId, propiedad, authentication.getName()));
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<Propiedad>> listarPropiedades(@PathVariable Long leadId) {
        return ResponseEntity.ok(propiedadService.obtenerPropiedadesDeLead(leadId));
    }

    @PostMapping("/{propiedadId}/eventos")
    public ResponseEntity<EventoPropiedad> registrarEvento(@PathVariable Long propiedadId,
                                                           @RequestBody EventoPropiedad evento) {
        return ResponseEntity.ok(propiedadService.registrarEvento(propiedadId, evento));
    }

    @GetMapping("/{propiedadId}/eventos")
    public ResponseEntity<List<EventoPropiedad>> listarEventos(@PathVariable Long propiedadId) {
        return ResponseEntity.ok(propiedadService.obtenerEventos(propiedadId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Propiedad> actualizarEstado(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        return ResponseEntity.ok(propiedadService.actualizarEstado(id, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Propiedad> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(propiedadService.obtenerPorId(id));
    }


}
