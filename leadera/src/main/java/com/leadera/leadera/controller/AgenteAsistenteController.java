package com.leadera.leadera.controller;

import com.leadera.leadera.dto.AgenteEquipoDTO;
import com.leadera.leadera.dto.AsistenteStatsDTO;
import com.leadera.leadera.dto.CambiarActivoRequest;
import com.leadera.leadera.dto.CrearAgenteEquipoRequest;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.service.AgenteContextResolver;
import com.leadera.leadera.service.InmobiliariaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Gestión de asistentes de un AGENTE (o DUENO). A diferencia de /inmobiliaria/**
// no usa @PreAuthorize por rol: la autoridad es la relación de supervisión y se
// valida método a método contra el actor resuelto del token.
@RestController
@RequestMapping("/agente/asistentes")
public class AgenteAsistenteController {

    private final InmobiliariaService inmobiliariaService;
    private final AgenteContextResolver agenteContextResolver;

    public AgenteAsistenteController(InmobiliariaService inmobiliariaService,
                                     AgenteContextResolver agenteContextResolver) {
        this.inmobiliariaService = inmobiliariaService;
        this.agenteContextResolver = agenteContextResolver;
    }

    // Un ASISTENTE no gestiona asistentes (no puede crear, listar ni desactivar).
    private Agente resolverSupervisor(Authentication authentication) {
        Agente actor = agenteContextResolver.resolverActor(authentication.getName());
        if (actor.getRol() == RolAgente.ASISTENTE) {
            throw new UnauthorizedActionException("Un asistente no puede gestionar asistentes.");
        }
        return actor;
    }

    @PostMapping
    public ResponseEntity<AgenteEquipoDTO> crearAsistente(@Valid @RequestBody CrearAgenteEquipoRequest request,
                                                          Authentication authentication) {
        Agente supervisor = resolverSupervisor(authentication);
        AgenteEquipoDTO creado = inmobiliariaService.crearAsistente(request, supervisor);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public List<AsistenteStatsDTO> listarAsistentes(Authentication authentication) {
        return inmobiliariaService.listarAsistentes(resolverSupervisor(authentication));
    }

    @PatchMapping("/{id}/activo")
    public AsistenteStatsDTO cambiarActivo(@PathVariable Long id,
                                           @Valid @RequestBody CambiarActivoRequest request,
                                           Authentication authentication) {
        Agente supervisor = resolverSupervisor(authentication);
        return inmobiliariaService.cambiarActivoAsistente(id, request.getActivo(), supervisor);
    }
}
