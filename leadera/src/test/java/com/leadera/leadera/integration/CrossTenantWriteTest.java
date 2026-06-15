package com.leadera.leadera.integration;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.entity.EventoOperacion;
import com.leadera.leadera.entity.Inmobiliaria;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.RolAgente;
import com.leadera.leadera.enums.TipoEvento;
import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.enums.TipoVivienda;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.BusquedaRepository;
import com.leadera.leadera.repository.EventoOperacionRepository;
import com.leadera.leadera.repository.InmobiliariaRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import com.leadera.leadera.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;

/**
 * Regresión del bug de escritura cross-tenant por entity binding: los POST de
 * creación aceptaban la entidad cruda con id settable, y un body con
 * {"id": <id ajeno>} convertía el save() en un merge sobre el registro de
 * otra inmobiliaria. Con los DTOs CrearPropiedadRequest / CrearEventoRequest
 * el id del body se ignora y siempre se inserta un registro nuevo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossTenantWriteTest {

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

    private Agente agenteA;
    private Agente agenteB;

    @BeforeEach
    void setUp() {
        eventoOperacionRepository.deleteAll();
        operacionRepository.deleteAll();
        busquedaRepository.deleteAll();
        propiedadRepository.deleteAll();
        leadRepository.deleteAll();
        agenteRepository.deleteAll();
        inmobiliariaRepository.deleteAll();

        Inmobiliaria inmobiliariaA = crearInmobiliaria("Inmobiliaria A");
        Inmobiliaria inmobiliariaB = crearInmobiliaria("Inmobiliaria B");
        agenteA = crearAgente("Ana", "DeA", "agente.a@test.com", inmobiliariaA);
        agenteB = crearAgente("Beto", "DeB", "agente.b@test.com", inmobiliariaB);
    }

    @Test
    void agenteB_no_puede_pisar_propiedad_de_agenteA() throws Exception {
        Lead leadA = crearLead(agenteA, "Vendedor", "DeA", "1111111111");
        Propiedad propiedadA = crearPropiedad(leadA, "Direccion Original 123");
        Lead leadB = crearLead(agenteB, "Vendedor", "DeB", "2222222222");

        // B manda el id de la propiedad de A en el body de creación.
        mockMvc.perform(post("/propiedades/lead/" + leadB.getId())
                        .header("Authorization", bearer(agenteB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "direccion": "Hackeado",
                                  "precio": 1,
                                  "zona": "Palermo",
                                  "tipoVivienda": "DEPTO"
                                }
                                """.formatted(propiedadA.getId())))
                .andExpect(status().isOk())
                // El id del body se ignora: se inserta una propiedad nueva.
                .andExpect(jsonPath("$.id").value(not((int) propiedadA.getId())));

        // La propiedad de A quedó intacta.
        Propiedad recargada = propiedadRepository.findById(propiedadA.getId()).orElseThrow();
        assertThat(recargada.getDireccion()).isEqualTo("Direccion Original 123");
        assertThat(recargada.getLead().getId()).isEqualTo(leadA.getId());

        mockMvc.perform(get("/propiedades/" + propiedadA.getId())
                        .header("Authorization", bearer(agenteA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Direccion Original 123"));
    }

    @Test
    void agenteB_no_puede_pisar_evento_de_operacion_de_agenteA() throws Exception {
        Lead leadA = crearLead(agenteA, "Comprador", "DeA", "3333333333");
        Operacion operacionA = crearOperacionCompra(agenteA, leadA);
        EventoOperacion eventoA = crearEvento(operacionA, TipoEvento.CONSULTA, "Detalle original");

        Lead leadB = crearLead(agenteB, "Comprador", "DeB", "4444444444");
        Operacion operacionB = crearOperacionCompra(agenteB, leadB);

        // B manda el id del evento de A en el body de creación.
        mockMvc.perform(post("/leads/" + leadB.getId() + "/operaciones/" + operacionB.getId() + "/eventos")
                        .header("Authorization", bearer(agenteB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "tipo": "VISITA",
                                  "detalle": "Hackeado"
                                }
                                """.formatted(eventoA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(not(eventoA.getId().intValue())));

        // El evento de A quedó intacto.
        EventoOperacion recargado = eventoOperacionRepository.findById(eventoA.getId()).orElseThrow();
        assertThat(recargado.getDetalle()).isEqualTo("Detalle original");
        assertThat(recargado.getTipo()).isEqualTo(TipoEvento.CONSULTA);
        assertThat(recargado.getOperacion().getId()).isEqualTo(operacionA.getId());
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

    private Agente crearAgente(String nombre, String apellido, String email, Inmobiliaria inmobiliaria) {
        Agente agente = new Agente();
        agente.setNombre(nombre);
        agente.setApellido(apellido);
        agente.setEmail(email);
        agente.setPassword(passwordEncoder.encode("password123"));
        agente.setRol(RolAgente.AGENTE);
        agente.setInmobiliaria(inmobiliaria);
        agente.setActivo(true);
        agente.setDebeCambiarPassword(false);
        return agenteRepository.save(agente);
    }

    private String bearer(Agente agente) {
        return "Bearer " + jwtService.generarToken(agente, agente.getId());
    }

    private Lead crearLead(Agente agente, String nombre, String apellido, String telefono) {
        Lead lead = new Lead();
        lead.setNombre(nombre);
        lead.setApellido(apellido);
        lead.setTelefono(telefono);
        lead.setEstado(EstadoLead.CALIENTE);
        lead.setFechaEntrada(LocalDateTime.now());
        lead.setAgente(agente);
        return leadRepository.save(lead);
    }

    private Propiedad crearPropiedad(Lead lead, String direccion) {
        Propiedad propiedad = new Propiedad();
        propiedad.setLead(lead);
        propiedad.setDireccion(direccion);
        propiedad.setZona("Palermo");
        propiedad.setTipoVivienda(TipoVivienda.DEPTO);
        propiedad.setPrecio(new BigDecimal("100000"));
        propiedad.setFechaPublicacion(LocalDateTime.now());
        return propiedadRepository.save(propiedad);
    }

    private Operacion crearOperacionCompra(Agente agente, Lead comprador) {
        Busqueda busqueda = new Busqueda();
        busqueda.setZona("Palermo");
        busqueda.setTipoVivienda(TipoVivienda.DEPTO);
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

    private EventoOperacion crearEvento(Operacion operacion, TipoEvento tipo, String detalle) {
        EventoOperacion evento = new EventoOperacion();
        evento.setOperacion(operacion);
        evento.setTipo(tipo);
        evento.setDetalle(detalle);
        evento.setFecha(LocalDateTime.now());
        return eventoOperacionRepository.save(evento);
    }

    @Test
    void crearOperacion_ignora_id_de_busqueda_en_body() throws Exception {
        Lead leadA = crearLead(agenteA, "Comprador", "DeA", "5555555555");
        Operacion operacionA = crearOperacionCompra(agenteA, leadA);
        Long busquedaAId = operacionA.getBusqueda().getId();

        Lead leadB = crearLead(agenteB, "Comprador", "DeB", "6666666666");

        // B manda en el body el id de la busqueda de A intentando pisar datos ajenos.
        // Con BusquedaData (sin campo id), Jackson lo ignora y se crea una busqueda nueva.
        mockMvc.perform(post("/leads/" + leadB.getId() + "/operaciones")
                        .header("Authorization", bearer(agenteB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Búsqueda de DEPTO",
                                  "tipoOperacion": "COMPRA",
                                  "descripcion": "test",
                                  "busqueda": {
                                    "zona": "Palermo",
                                    "tipoVivienda": "DEPTO",
                                    "id": %d
                                  }
                                }
                                """.formatted(busquedaAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoOperacion").value("COMPRA"))
                .andExpect(jsonPath("$.leadId").value(leadB.getId().intValue()));

        // La busqueda de A sigue intacta.
        assertThat(busquedaRepository.findById(busquedaAId)).isPresent();
        assertThat(busquedaRepository.findById(busquedaAId).get().getZona()).isEqualTo("Palermo");
    }

    @Test
    void crearOperacion_devuelve_OperacionDTO_sin_exponer_entity() throws Exception {
        Lead leadA = crearLead(agenteA, "Comprador", "DeA", "7777777777");

        String response = mockMvc.perform(post("/leads/" + leadA.getId() + "/operaciones")
                        .header("Authorization", bearer(agenteA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Búsqueda test",
                                  "tipoOperacion": "COMPRA",
                                  "descripcion": "desc",
                                  "busqueda": {
                                    "zona": "Microcentro",
                                    "tipoVivienda": "DEPTO"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Búsqueda test"))
                .andExpect(jsonPath("$.estadoOperacion").value("PUBLICADA"))
                .andExpect(jsonPath("$.leadId").value(leadA.getId().intValue()))
                .andReturn().getResponse().getContentAsString();

        // La entity no debe exponer el campo "agente" ni "lead" completos.
        assertThat(response).doesNotContain("\"agente\":{");
        assertThat(response).doesNotContain("\"lead\":{");
    }
}
