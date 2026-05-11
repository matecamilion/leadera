package com.leadera.leadera.service;

import com.leadera.leadera.model.Agente;
import com.leadera.leadera.model.LoginResponse;
import com.leadera.leadera.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new RuntimeException("Agente no encontrado con email: " + email));

        String token = jwtService.generarToken(agente, agente.getId());

        return new LoginResponse(
                token,
                agente.getNombre(),
                agente.getApellido(),
                agente.getEmail()
        );
    }

    public String registrar(Agente agente) {
        //Encriptamos contraseña antes de guardar
        agente.setPassword(passwordEncoder.encode(agente.getPassword()));
        agenteRepository.save(agente);
        return "Agente registrado con exito";
    }


}
