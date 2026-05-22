package com.leadera.leadera.controller;

import com.leadera.leadera.dto.ActualizarPerfilRequest;
import com.leadera.leadera.dto.AgentePerfilDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agente")
public class AgenteController {

    private final AgenteRepository agenteRepository;

    public AgenteController(AgenteRepository agenteRepository) {
        this.agenteRepository = agenteRepository;
    }

    @GetMapping("/perfil")
    public ResponseEntity<AgentePerfilDTO> obtenerPerfil(Authentication authentication) {
        Agente agente = buscarAgente(authentication.getName());
        return ResponseEntity.ok(toDTO(agente));
    }

    @PatchMapping("/perfil")
    public ResponseEntity<AgentePerfilDTO> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request,
                                                            Authentication authentication) {
        Agente agente = buscarAgente(authentication.getName());
        agente.setNombre(request.getNombre());
        agente.setApellido(request.getApellido());
        agente.setMetaMensualCierres(request.getMetaMensualCierres());
        return ResponseEntity.ok(toDTO(agenteRepository.save(agente)));
    }

    private Agente buscarAgente(String email) {
        return agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));
    }

    private AgentePerfilDTO toDTO(Agente a) {
        return new AgentePerfilDTO(
                a.getId(), a.getNombre(), a.getApellido(), a.getEmail(), a.getMetaMensualCierres()
        );
    }
}
