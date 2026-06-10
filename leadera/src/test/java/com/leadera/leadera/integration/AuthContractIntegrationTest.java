package com.leadera.leadera.integration;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Inmobiliaria;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.BusquedaRepository;
import com.leadera.leadera.repository.EventoOperacionRepository;
import com.leadera.leadera.repository.InmobiliariaRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import com.leadera.leadera.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de autenticación: todo problema de AUTENTICACIÓN devuelve
 * 401 (el error-interceptor del frontend desloguea y redirige a /login solo
 * ante 401). El 403 queda reservado a problemas de AUTORIZACIÓN (rol).
 *
 * Regresión de dos bugs: token vencido daba 500 (ExpiredJwtException sin
 * capturar en JwtAuthenticationFilter) y request sin token daba 403 (sin
 * authenticationEntryPoint en SecurityConfig).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthContractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private InmobiliariaRepository inmobiliariaRepository;
    @Autowired private AgenteRepository agenteRepository;
    @Autowired private LeadRepository leadRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private OperacionRepository operacionRepository;
    @Autowired private BusquedaRepository busquedaRepository;
    @Autowired private EventoOperacionRepository eventoOperacionRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private Agente agente;

    @BeforeEach
    void setUp() {
        eventoOperacionRepository.deleteAll();
        operacionRepository.deleteAll();
        busquedaRepository.deleteAll();
        propiedadRepository.deleteAll();
        leadRepository.deleteAll();
        agenteRepository.deleteAll();
        inmobiliariaRepository.deleteAll();

        Inmobiliaria inmobiliaria = new Inmobiliaria();
        inmobiliaria.setNombre("Inmobiliaria Auth");
        inmobiliaria.setFechaCreacion(LocalDateTime.now());
        inmobiliaria = inmobiliariaRepository.save(inmobiliaria);

        agente = new Agente();
        agente.setNombre("Aldo");
        agente.setApellido("Auth");
        agente.setEmail("aldo.auth@test.com");
        agente.setPassword(passwordEncoder.encode("password123"));
        agente.setRol(RolAgente.AGENTE);
        agente.setInmobiliaria(inmobiliaria);
        agente.setActivo(true);
        agente.setDebeCambiarPassword(false);
        agente = agenteRepository.save(agente);
    }

    @Test
    void sinTokenDevuelve401ConJson() throws Exception {
        mockMvc.perform(get("/propiedades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autenticación requerida"));
    }

    @Test
    void tokenVencidoDevuelve401EnVezDe500() throws Exception {
        // Token firmado con el mismo secret pero expirado hace una hora.
        String tokenVencido = Jwts.builder()
                .setSubject(agente.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                .setExpiration(new Date(System.currentTimeMillis() - 60 * 60 * 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/propiedades")
                        .header("Authorization", "Bearer " + tokenVencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token inválido o sesión expirada"));
    }

    @Test
    void tokenMalformadoDevuelve401() throws Exception {
        mockMvc.perform(get("/propiedades")
                        .header("Authorization", "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token inválido o sesión expirada"));
    }

    @Test
    void tokenDeAgenteDesactivadoDevuelve401() throws Exception {
        String token = jwtService.generarToken(agente, agente.getId());
        agente.setActivo(false);
        agenteRepository.save(agente);

        mockMvc.perform(get("/propiedades")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoSigueFuncionando() throws Exception {
        mockMvc.perform(get("/propiedades")
                        .header("Authorization", "Bearer " + jwtService.generarToken(agente, agente.getId())))
                .andExpect(status().isOk());
    }
}
