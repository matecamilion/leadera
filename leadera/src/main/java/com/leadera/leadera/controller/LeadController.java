package com.leadera.leadera.controller;

import com.leadera.leadera.dto.ActividadRecienteDTO;
import com.leadera.leadera.dto.AgenteDashboardDTO;
import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.DashboardDTO;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.dto.LeadResumenDTO;
import com.leadera.leadera.dto.LeadsHoyResponse;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.service.DashboardService;
import com.leadera.leadera.service.LeadService;
import jakarta.validation.Valid;
import lombok.Getter;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leads")
public class LeadController {
    private final LeadService leadService;
    private final DashboardService dashboardService;

    public LeadController(LeadService leadService, DashboardService dashboardService) {
        this.leadService = leadService;
        this.dashboardService = dashboardService;
    }

    //Crear lead
    @PostMapping
    public ResponseEntity<LeadResponseDTO> crearLead(@Valid @RequestBody CrearLeadRequest request, Authentication authentication) {
        LeadResponseDTO creado = leadService.crearLead(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Obtener mis leads (paginado)
    @GetMapping
    public Page<LeadResumenDTO> listarLeads(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return leadService.obtenerResumenLeadsPorAgente(authentication.getName(), PageRequest.of(page, size));
    }

    // Obtener lead por id (Le pasamos el email para verificar que sea el dueño)
    @GetMapping("/{id}")
    public LeadDetalleResponse obtenerLeadPorId(@PathVariable Long id, Authentication authentication) {
        return leadService.obtenerLeadsPorId(id, authentication.getName());
    }

    // Obtener leads por estado (Agregamos Authentication)
    @GetMapping("/estado/{estado}")
    public List<Lead> obtenerLeadsPorEstado(@PathVariable EstadoLead estado, Authentication authentication) {
        return leadService.obtenerLeadsPorEstado(estado, authentication.getName());
    }

    // Obtener leads que nunca fueron contactados (Agregamos Authentication)
    @GetMapping("/sin-contactar")
    public List<Lead> obtenerLeadsSinContacto(Authentication authentication) {
        return leadService.obtenerLeadsSinContacto(authentication.getName());
    }

    @GetMapping("/{id}/interacciones")
    public List<Interaccion> obtenerInteraccionesPorId(@PathVariable Long id) {
        // Aquí podrías agregar seguridad también si quisieras
        return leadService.obtenerHistorialInteracciones(id);
    }

    @GetMapping("/inactivos")
    public List<Lead> obtenerLeadsInactivosPorDias(@RequestParam Integer dias, Authentication authentication) {
        return leadService.obtenerLeadsInactivos(dias, authentication.getName());
    }

    @GetMapping("/prioritarios")
    public List<Lead> obtenerLeadsPrioritarios(@RequestParam Integer dias, Authentication authentication) {
        return leadService.obtenerLeadsPrioritarios(dias, authentication.getName());
    }

    // Actualizar estado del lead
    @PutMapping("/{id}/estado")
    public Lead cambiarEstado(@PathVariable Long id, @RequestParam EstadoLead nuevoEstado, Authentication authentication) {
        // El service ya maneja la búsqueda por ID
        return leadService.cambiarEstado(id, nuevoEstado, authentication.getName());
    }


    @GetMapping("/hoy")
    public LeadsHoyResponse obtenerLeadsDeHoy(Authentication authentication) {
        return leadService.obtenerLeadsDeHoy(authentication.getName());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Lead> establecerLeadInactivo(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(leadService.establecerLeadInactivo(id, authentication.getName()));
    }

    @GetMapping("/agente/{id}/stats")
    public ResponseEntity<AgenteDashboardDTO> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.obtenerEstadisticasAgente(id));
    }

    @GetMapping("/agente/{id}/actividad-reciente")
    public ResponseEntity<List<ActividadRecienteDTO>> getActividadReciente(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.obtenerActividadReciente(id, 5));
    }

    @GetMapping("/agente/{id}/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(
            @PathVariable Long id,
            @RequestParam(name = "periodo", defaultValue = "30d") String periodo) {
        return ResponseEntity.ok(dashboardService.obtenerDashboard(id, periodo));
    }

    @PutMapping("/{id}/editar-contacto")
    public ResponseEntity<Lead> editarContacto(@PathVariable Long id, @RequestBody Lead nuevosDatos, Authentication authentication) {
        Lead leadActualizado = leadService.editarInfoContacto(id, nuevosDatos.getTelefono(), nuevosDatos.getEmail(), authentication.getName());
        return ResponseEntity.ok(leadActualizado);
    }



}
