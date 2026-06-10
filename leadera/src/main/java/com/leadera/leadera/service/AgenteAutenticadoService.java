package com.leadera.leadera.service;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

// Helper central para resolver el Agente autenticado (con su inmobiliaria,
// que es EAGER en el @ManyToOne). Evita repetir findByEmail por todos lados.
@Service
@RequiredArgsConstructor
public class AgenteAutenticadoService {

    private final AgenteRepository agenteRepository;

    public Agente obtenerAgenteAutenticado(Authentication authentication) {
        // El JwtAuthenticationFilter ya cargó la entidad Agente como principal
        // en este mismo request: reutilizarla evita una query extra.
        if (authentication.getPrincipal() instanceof Agente agente) {
            return agente;
        }
        return obtenerPorEmail(authentication.getName());
    }

    public Agente obtenerPorEmail(String email) {
        return agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));
    }
}
