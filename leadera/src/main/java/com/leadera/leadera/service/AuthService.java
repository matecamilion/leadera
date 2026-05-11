package com.leadera.leadera.service;

import com.leadera.leadera.dto.LoginResponse;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AgenteRepository agenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(String email, String password) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        Agente agente = agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        String token = jwtService.generarToken(agente, agente.getId());

        return new LoginResponse(
                token,
                agente.getNombre(),
                agente.getApellido(),
                agente.getEmail()
        );
    }

    public String registrar(Agente agente) {
        if (agente.getEmail() != null && agenteRepository.findByEmail(agente.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }
        agente.setPassword(passwordEncoder.encode(agente.getPassword()));
        try {
            agenteRepository.save(agente);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }
        return "Agente registrado con éxito";
    }


}
