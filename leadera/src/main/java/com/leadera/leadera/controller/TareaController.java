package com.leadera.leadera.controller;

import com.leadera.leadera.dto.AsistenteResumenDTO;
import com.leadera.leadera.dto.CrearTareaRequest;
import com.leadera.leadera.dto.TareaDTO;
import com.leadera.leadera.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tareas")
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    // El AGENTE/DUENO crea una tarea para uno de sus asistentes.
    @PostMapping
    public ResponseEntity<TareaDTO> crear(@Valid @RequestBody CrearTareaRequest request,
                                          Authentication authentication) {
        TareaDTO creada = tareaService.crearTarea(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // El actor (típicamente el asistente) ve sus tareas de hoy; las cuotas se auto-completan.
    @GetMapping("/hoy")
    public List<TareaDTO> tareasDeHoy(Authentication authentication) {
        return tareaService.obtenerTareasDelDia(authentication.getName());
    }

    // Marca a mano una tarea discreta propia.
    @PatchMapping("/{id}/completar")
    public TareaDTO completar(@PathVariable Long id, Authentication authentication) {
        return tareaService.completarTarea(id, authentication.getName());
    }

    // Asistentes del actor: alimenta el selector y la visibilidad del link en el sidebar.
    @GetMapping("/mis-asistentes")
    public List<AsistenteResumenDTO> misAsistentes(Authentication authentication) {
        return tareaService.obtenerMisAsistentes(authentication.getName());
    }

    // El supervisor consulta las tareas que le creó a un asistente suyo.
    @GetMapping("/asistente/{asistenteId}")
    public List<TareaDTO> tareasDeAsistente(@PathVariable Long asistenteId,
                                            Authentication authentication) {
        return tareaService.obtenerTareasDeAsistente(asistenteId, authentication.getName());
    }
}
