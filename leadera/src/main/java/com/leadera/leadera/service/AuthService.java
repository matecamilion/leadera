package com.leadera.leadera.service;

import com.leadera.leadera.dto.CambiarPasswordRequest;
import com.leadera.leadera.dto.LoginResponse;
import com.leadera.leadera.dto.RegisterRequest;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Inmobiliaria;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.InmobiliariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AgenteRepository agenteRepository;
    private final InmobiliariaRepository inmobiliariaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ZoneId zonaHoraria;

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
                agente.getEmail(),
                agente.getRol() != null ? agente.getRol().name() : RolAgente.AGENTE.name(),
                agente.isDebeCambiarPassword()
        );
    }

    // Registro público: quien se registra crea su propia inmobiliaria y queda
    // como DUENO. Transaccional: si falla el agente, no queda la inmobiliaria
    // huérfana.
    @Transactional
    public String registrar(RegisterRequest request) {
        if (request.getEmail() != null && agenteRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }

        Inmobiliaria inmobiliaria = new Inmobiliaria();
        inmobiliaria.setNombre(request.getNombreInmobiliaria().trim());
        inmobiliaria.setFechaCreacion(LocalDateTime.now(zonaHoraria));
        inmobiliariaRepository.save(inmobiliaria);

        Agente agente = new Agente();
        agente.setNombre(request.getNombre());
        agente.setApellido(request.getApellido());
        agente.setEmail(request.getEmail());
        agente.setPassword(passwordEncoder.encode(request.getPassword()));
        agente.setInmobiliaria(inmobiliaria);
        agente.setRol(RolAgente.DUENO);
        agente.setActivo(true);
        agente.setDebeCambiarPassword(false);
        try {
            agenteRepository.save(agente);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Ya existe un agente con ese email");
        }
        return "Agente registrado con éxito";
    }

    // Cambio de password autenticado. Lo usa el flujo de password temporal
    // (debeCambiarPassword) pero sirve para cualquier cambio voluntario.
    public void cambiarPassword(String email, CambiarPasswordRequest request) {
        Agente agente = agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        if (!passwordEncoder.matches(request.getPasswordActual(), agente.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }
        if (request.getPasswordActual().equals(request.getPasswordNueva())) {
            throw new BadRequestException("La contraseña nueva debe ser distinta de la actual");
        }

        agente.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        agente.setDebeCambiarPassword(false);
        agenteRepository.save(agente);
    }

}
