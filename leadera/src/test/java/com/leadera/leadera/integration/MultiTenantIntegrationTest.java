package com.leadera.leadera.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.entity.Inmobiliaria;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.enums.TipoVivienda;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.BusquedaRepository;
import com.leadera.leadera.repository.InmobiliariaRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import com.leadera.leadera.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del multi-tenant por inmobiliaria. Cada test crea sus
 * propias inmobiliarias y agentes de prueba en H2: NO asume nada sobre los
 * agentes reales de la base (que tras la migración quedan cada uno como
 * DUENO de su propia inmobiliaria individual).
 *
 * Escenario base:
 *  - Inmobiliaria A: duenoA + agenteA (mismo equipo).
 *  - Inmobiliaria B: duenoB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenantIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CacheManager cacheManager;

    @Autowired private InmobiliariaRepository inmobiliariaRepository;
    @Autowired private AgenteRepository agenteRepository;
    @Autowired private LeadRepository leadRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private OperacionRepository operacionRepository;
    @Autowired private BusquedaRepository busquedaRepository;

    private Inmobiliaria inmobiliariaA;
    private Inmobiliaria inmobiliariaB;
    private Agente duenoA;
    private Agente agenteA;
    private Agente duenoB;

    @BeforeEach
    void setUp() {
        // Orden de borrado acorde a las FKs: operacion -> busqueda/propiedad -> lead -> agente -> inmobiliaria.
        operacionRepository.deleteAll();
        busquedaRepository.deleteAll();
        propiedadRepository.deleteAll();
        leadRepository.deleteAll();
        agenteRepository.deleteAll();
        inmobiliariaRepository.deleteAll();

        Cache cache = cacheManager.getCache("estadisticasAgente");
        if (cache != null) {
            cache.clear();
        }

        inmobiliariaA = crearInmobiliaria("Inmobiliaria A");
        inmobiliariaB = crearInmobiliaria("Inmobiliaria B");
        duenoA = crearAgente("Diego", "Dueno", "dueno.a@test.com", RolAgente.DUENO, inmobiliariaA);
        agenteA = crearAgente("Carla", "Gomez", "agente.a@test.com", RolAgente.AGENTE, inmobiliariaA);
        duenoB = crearAgente("Berta", "Bsas", "dueno.b@test.com", RolAgente.DUENO, inmobiliariaB);
    }

    // =========================================================
    // 1. Aislamiento entre inmobiliarias
    // =========================================================
    @Nested
    class AislamientoEntreInmobiliarias {

        @Test
        void agenteDeOtraInmobiliariaNoPuedeVerUnLeadAjeno() throws Exception {
            Lead leadDeA = crearLead(duenoA, "Juan", "Perez", "1111111111", "juan@test.com");

            mockMvc.perform(get("/leads/" + leadDeA.getId())
                            .header("Authorization", bearer(duenoB)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void elMatchingNoCruzaInmobiliarias() throws Exception {
            // Propiedad en A; comprador con búsqueda compatible pero en B.
            Lead vendedorA = crearLead(duenoA, "Vito", "Vendedor", "2222222222", null);
            Propiedad propiedad = crearPropiedadDisponible(vendedorA);
            Lead compradorB = crearLead(duenoB, "Carlos", "Comprador", "3333333333", null);
            crearOperacionCompraConBusqueda(duenoB, compradorB);

            mockMvc.perform(get("/propiedades/" + propiedad.getId() + "/compradores-potenciales")
                            .header("Authorization", bearer(duenoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void elListadoDeLeadsDelEquipoNoIncluyeOtrasInmobiliarias() throws Exception {
            crearLead(agenteA, "Lead", "DeA", "4444444444", null);
            crearLead(duenoB, "Lead", "DeB", "5555555555", null);

            mockMvc.perform(get("/inmobiliaria/leads")
                            .header("Authorization", bearer(duenoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].lead.apellido").value("DeA"));
        }

        @Test
        void lasStatsDelEquipoSoloIncluyenAgentesPropios() throws Exception {
            crearLead(agenteA, "Lead", "DeA", "4444444444", null);
            crearLead(duenoB, "Lead", "DeB", "5555555555", null);

            mockMvc.perform(get("/inmobiliaria/stats")
                            .header("Authorization", bearer(duenoA)))
                    .andExpect(status().isOk())
                    // A tiene dueño + agente; B no aparece.
                    .andExpect(jsonPath("$.porAgente", hasSize(2)))
                    .andExpect(jsonPath("$.totales.activos").value(1))
                    .andExpect(content().string(not(containsString(duenoB.getNombre()))));
        }

        @Test
        void unDuenoNoPuedeDesactivarAgentesDeOtraInmobiliaria() throws Exception {
            mockMvc.perform(patch("/inmobiliaria/agentes/" + agenteA.getId() + "/activo")
                            .header("Authorization", bearer(duenoB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isNotFound());

            // El agente de A sigue activo.
            assertThat(agenteRepository.findById(agenteA.getId()).orElseThrow().isActivo()).isTrue();
        }
    }

    // =========================================================
    // 2. Rol AGENTE: 403 en toda la gestión de /inmobiliaria/**
    // =========================================================
    @Nested
    class PermisosPorRol {

        @Test
        void unAgenteRecibeForbiddenEnTodosLosEndpointsDeGestion() throws Exception {
            String token = bearer(agenteA);

            mockMvc.perform(get("/inmobiliaria/agentes").header("Authorization", token))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/inmobiliaria/agentes")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Nuevo", "apellido", "Agente",
                                    "email", "nuevo@test.com", "passwordTemporal", "temporal123"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(patch("/inmobiliaria/agentes/" + duenoA.getId() + "/activo")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/inmobiliaria/leads").header("Authorization", token))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/inmobiliaria/stats").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unDuenoNoPuedeDesactivarseASiMismo() throws Exception {
            mockMvc.perform(patch("/inmobiliaria/agentes/" + duenoA.getId() + "/activo")
                            .header("Authorization", bearer(duenoA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\": false}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================
    // 3. Matching compartido dentro de la misma inmobiliaria
    // =========================================================
    @Nested
    class MatchingCompartido {

        @Test
        void elMatchingDevuelveLeadsDeOtroAgenteDelEquipoSinDatosDeContacto() throws Exception {
            String telefonoComprador = "1199887766";
            String emailComprador = "comprador@test.com";

            // duenoA publica la propiedad; el comprador es de agenteA (mismo equipo).
            Lead vendedor = crearLead(duenoA, "Vito", "Vendedor", "2222222222", null);
            Propiedad propiedad = crearPropiedadDisponible(vendedor);
            Lead comprador = crearLead(agenteA, "Cosme", "Fulanito", telefonoComprador, emailComprador);
            crearOperacionCompraConBusqueda(agenteA, comprador);

            mockMvc.perform(get("/propiedades/" + propiedad.getId() + "/compradores-potenciales")
                            .header("Authorization", bearer(duenoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombreLead").value("Cosme"))
                    .andExpect(jsonPath("$[0].apellidoLead").value("Fulanito"))
                    .andExpect(jsonPath("$[0].estadoLead").value("CALIENTE"))
                    .andExpect(jsonPath("$[0].esMio").value(false))
                    .andExpect(jsonPath("$[0].agenteId").value(agenteA.getId()))
                    .andExpect(jsonPath("$[0].nombreAgente").value("Carla"))
                    .andExpect(jsonPath("$[0].apellidoAgente").value("Gomez"))
                    // Nunca viajan los datos de contacto del lead ajeno.
                    .andExpect(jsonPath("$[0].telefono").doesNotExist())
                    .andExpect(jsonPath("$[0].leadTelefono").doesNotExist())
                    .andExpect(jsonPath("$[0].email").doesNotExist())
                    .andExpect(content().string(not(containsString(telefonoComprador))))
                    .andExpect(content().string(not(containsString(emailComprador))));
        }

        @Test
        void elMatchingMarcaEsMioCuandoElCompradorEsPropio() throws Exception {
            Lead vendedor = crearLead(duenoA, "Vito", "Vendedor", "2222222222", null);
            Propiedad propiedad = crearPropiedadDisponible(vendedor);
            Lead compradorPropio = crearLead(duenoA, "Mio", "Propio", "6666666666", null);
            crearOperacionCompraConBusqueda(duenoA, compradorPropio);

            mockMvc.perform(get("/propiedades/" + propiedad.getId() + "/compradores-potenciales")
                            .header("Authorization", bearer(duenoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].esMio").value(true));
        }
    }

    // =========================================================
    // 4. Duplicados a nivel inmobiliaria
    // =========================================================
    @Nested
    class DuplicadosPorInmobiliaria {

        @Test
        void telefonoDuplicadoEntreAgentesDelMismoEquipoDa409ConElNombreDelAgente() throws Exception {
            crearLead(agenteA, "Original", "DeCarla", "1155550000", null);

            mockMvc.perform(post("/leads")
                            .header("Authorization", bearer(duenoA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Duplicado", "apellido", "Nuevo",
                                    "telefono", "1155550000"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Carla Gomez")))
                    .andExpect(jsonPath("$.message", containsString("tu inmobiliaria")));
        }

        @Test
        void elMismoTelefonoEnOtraInmobiliariaNoEsDuplicado() throws Exception {
            crearLead(agenteA, "Original", "DeCarla", "1155550000", null);

            mockMvc.perform(post("/leads")
                            .header("Authorization", bearer(duenoB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Mismo", "apellido", "Telefono",
                                    "telefono", "1155550000"))))
                    .andExpect(status().isCreated());
        }
    }

    // =========================================================
    // 5. Registro público: inmobiliaria + dueño atómicos
    // =========================================================
    @Nested
    class RegistroPublico {

        @Test
        void elRegistroCreaInmobiliariaYDuenoJuntos() throws Exception {
            long inmobiliariasAntes = inmobiliariaRepository.count();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Nora", "apellido", "Nueva",
                                    "email", "nora@test.com", "password", "password123",
                                    "nombreInmobiliaria", "Inmobiliaria Nueva"))))
                    .andExpect(status().isCreated());

            Agente creado = agenteRepository.findByEmail("nora@test.com").orElseThrow();
            assertThat(creado.getRol()).isEqualTo(RolAgente.DUENO);
            assertThat(creado.isActivo()).isTrue();
            assertThat(creado.isDebeCambiarPassword()).isFalse();
            assertThat(creado.getInmobiliaria()).isNotNull();
            assertThat(creado.getInmobiliaria().getNombre()).isEqualTo("Inmobiliaria Nueva");
            assertThat(inmobiliariaRepository.count()).isEqualTo(inmobiliariasAntes + 1);
        }

        @Test
        void sinNombreDeInmobiliariaElRegistroFallaSinDejarDatosHuerfanos() throws Exception {
            long inmobiliariasAntes = inmobiliariaRepository.count();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Nora", "apellido", "Nueva",
                                    "email", "nora@test.com", "password", "password123"))))
                    .andExpect(status().isBadRequest());

            assertThat(agenteRepository.findByEmail("nora@test.com")).isEmpty();
            assertThat(inmobiliariaRepository.count()).isEqualTo(inmobiliariasAntes);
        }
    }

    // =========================================================
    // 6. Cambio de password con password temporal
    // =========================================================
    @Nested
    class CambioDePassword {

        @Test
        void unAgenteConPasswordTemporalPuedeCambiarlaConSuToken() throws Exception {
            // debeCambiarPassword=true NO debe bloquear este endpoint: es
            // justamente el que destraba el flag.
            agenteA.setDebeCambiarPassword(true);
            agenteRepository.save(agenteA);

            mockMvc.perform(post("/auth/cambiar-password")
                            .header("Authorization", bearer(agenteA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "passwordActual", "password123",
                                    "passwordNueva", "nuevaPassword456"))))
                    .andExpect(status().isOk());

            Agente actualizado = agenteRepository.findById(agenteA.getId()).orElseThrow();
            assertThat(actualizado.isDebeCambiarPassword()).isFalse();
            assertThat(passwordEncoder.matches("nuevaPassword456", actualizado.getPassword())).isTrue();
        }

        @Test
        void sinTokenElCambioDePasswordEsRechazado() throws Exception {
            mockMvc.perform(post("/auth/cambiar-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "passwordActual", "password123",
                                    "passwordNueva", "nuevaPassword456"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================
    // Helpers de datos de prueba
    // =========================================================

    private Inmobiliaria crearInmobiliaria(String nombre) {
        Inmobiliaria inmobiliaria = new Inmobiliaria();
        inmobiliaria.setNombre(nombre);
        inmobiliaria.setFechaCreacion(LocalDateTime.now());
        return inmobiliariaRepository.save(inmobiliaria);
    }

    private Agente crearAgente(String nombre, String apellido, String email,
                               RolAgente rol, Inmobiliaria inmobiliaria) {
        Agente agente = new Agente();
        agente.setNombre(nombre);
        agente.setApellido(apellido);
        agente.setEmail(email);
        agente.setPassword(passwordEncoder.encode("password123"));
        agente.setRol(rol);
        agente.setInmobiliaria(inmobiliaria);
        agente.setActivo(true);
        agente.setDebeCambiarPassword(false);
        return agenteRepository.save(agente);
    }

    private String bearer(Agente agente) {
        return "Bearer " + jwtService.generarToken(agente, agente.getId());
    }

    private Lead crearLead(Agente agente, String nombre, String apellido, String telefono, String email) {
        Lead lead = new Lead();
        lead.setNombre(nombre);
        lead.setApellido(apellido);
        lead.setTelefono(telefono);
        lead.setEmail(email);
        lead.setEstado(EstadoLead.CALIENTE);
        lead.setFechaEntrada(LocalDateTime.now());
        lead.setAgente(agente);
        return leadRepository.save(lead);
    }

    private Propiedad crearPropiedadDisponible(Lead lead) {
        Propiedad propiedad = new Propiedad();
        propiedad.setLead(lead);
        propiedad.setDireccion("Av. Siempreviva 742");
        propiedad.setZona("Palermo");
        propiedad.setTipoVivienda(TipoVivienda.DEPTO);
        propiedad.setPrecio(new BigDecimal("100000"));
        propiedad.setCantidadAmbientes(3);
        propiedad.setMetrosTotales(80);
        propiedad.setFechaPublicacion(LocalDateTime.now());
        return propiedadRepository.save(propiedad);
    }

    // Operación COMPRA con búsqueda compatible con crearPropiedadDisponible.
    private Operacion crearOperacionCompraConBusqueda(Agente agente, Lead comprador) {
        Busqueda busqueda = new Busqueda();
        busqueda.setZona("Palermo");
        busqueda.setTipoVivienda(TipoVivienda.DEPTO);
        busqueda.setPrecioMax(new BigDecimal("120000"));
        busqueda = busquedaRepository.save(busqueda);

        Operacion operacion = new Operacion();
        operacion.setTitulo("Búsqueda de depto");
        operacion.setTipoOperacion(TipoOperacion.COMPRA);
        operacion.setEstadoOperacion(EstadoOperacion.PUBLICADA);
        operacion.setLead(comprador);
        operacion.setAgente(agente);
        operacion.setBusqueda(busqueda);
        operacion.setFechaCreacion(LocalDateTime.now());
        return operacionRepository.save(operacion);
    }
}
