package com.leadera.leadera.controller;

import com.leadera.leadera.model.*;
import com.leadera.leadera.service.LeadService;
import lombok.Getter;
import org.apache.coyote.Response;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/leads")
public class LeadController {
    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    //Crear lead
    @PostMapping
    public Lead crearLead(@RequestBody Lead lead, Authentication authentication) {
        return leadService.crearLead(lead, authentication.getName());
    }

    // Obtener mis leads
    @GetMapping
    public List<LeadResumenDTO> listarLeads(Authentication authentication) {
        return leadService.obtenerResumenLeadsPorAgente(authentication.getName());
    }

    // Obtener lead por id (Le pasamos el email para verificar que sea el dueño)
    @GetMapping("/{id}")
    public Lead obtenerLeadPorId(@PathVariable Long id, Authentication authentication) {
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
    public Lead cambiarEstado(@PathVariable Long id, @RequestParam EstadoLead nuevoEstado) {
        // El service ya maneja la búsqueda por ID
        return leadService.cambiarEstado(id, nuevoEstado);
    }


    @GetMapping("/hoy")
    public LeadsHoyResponse obtenerLeadsDeHoy(Authentication authentication) {
        return leadService.obtenerLeadsDeHoy(authentication.getName());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Lead> establecerLeadInactivo(@PathVariable Long id) {
        try{
            Lead lead = leadService.establecerLeadInactivo(id);
            return ResponseEntity.ok(lead);
        }  catch (Exception e) {
            // Esto va a imprimir el error real en la consola de IntelliJ/Spring
            e.printStackTrace();
            // Cambiamos temporalmente a 500 para saber que el problema es del servidor y no de la URL
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/agente/{id}/stats")
    public ResponseEntity<AgenteDashboardDTO> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.obtenerEstadisticasAgente(id));
    }

    @PutMapping("/{id}/editar-contacto")
    public ResponseEntity<Lead>  editarContacto(@PathVariable Long id, @RequestBody Lead nuevosDatos, Authentication authentication) {
        try{
            Lead leadActualizado = leadService.editarInfoContacto(id, nuevosDatos.getTelefono(), nuevosDatos.getEmail(), authentication.getName());
            return ResponseEntity.ok(leadActualizado);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }



}
