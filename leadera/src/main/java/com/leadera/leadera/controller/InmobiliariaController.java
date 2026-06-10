package com.leadera.leadera.controller;

import com.leadera.leadera.dto.AgenteEquipoDTO;
import com.leadera.leadera.dto.CambiarActivoRequest;
import com.leadera.leadera.dto.CrearAgenteEquipoRequest;
import com.leadera.leadera.dto.EquipoStatsDTO;
import com.leadera.leadera.dto.LeadEquipoDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.service.AgenteAutenticadoService;
import com.leadera.leadera.service.InmobiliariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Gestión del equipo: solo el DUENO de la inmobiliaria puede usar estos
// endpoints (el @PreAuthorize a nivel clase aplica a todos los métodos).
@RestController
@RequestMapping("/inmobiliaria")
@PreAuthorize("hasRole('DUENO')")
@RequiredArgsConstructor
public class InmobiliariaController {

    private final InmobiliariaService inmobiliariaService;
    private final AgenteAutenticadoService agenteAutenticadoService;

    @PostMapping("/agentes")
    public ResponseEntity<AgenteEquipoDTO> crearAgente(@Valid @RequestBody CrearAgenteEquipoRequest request,
                                                       Authentication authentication) {
        Agente dueno = agenteAutenticadoService.obtenerAgenteAutenticado(authentication);
        AgenteEquipoDTO creado = inmobiliariaService.crearAgente(request, dueno);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/agentes")
    public ResponseEntity<List<AgenteEquipoDTO>> listarEquipo(Authentication authentication) {
        Agente dueno = agenteAutenticadoService.obtenerAgenteAutenticado(authentication);
        return ResponseEntity.ok(inmobiliariaService.listarEquipo(dueno));
    }

    @PatchMapping("/agentes/{id}/activo")
    public ResponseEntity<AgenteEquipoDTO> cambiarActivo(@PathVariable Long id,
                                                         @Valid @RequestBody CambiarActivoRequest request,
                                                         Authentication authentication) {
        Agente dueno = agenteAutenticadoService.obtenerAgenteAutenticado(authentication);
        return ResponseEntity.ok(inmobiliariaService.cambiarActivo(id, request.getActivo(), dueno));
    }

    @GetMapping("/leads")
    public Page<LeadEquipoDTO> listarLeadsDelEquipo(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Agente dueno = agenteAutenticadoService.obtenerAgenteAutenticado(authentication);
        return inmobiliariaService.obtenerLeadsDelEquipo(dueno, PageRequest.of(page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<EquipoStatsDTO> obtenerStats(Authentication authentication) {
        Agente dueno = agenteAutenticadoService.obtenerAgenteAutenticado(authentication);
        return ResponseEntity.ok(inmobiliariaService.obtenerStats(dueno));
    }
}
