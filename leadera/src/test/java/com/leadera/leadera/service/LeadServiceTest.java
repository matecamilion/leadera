package com.leadera.leadera.service;

import com.leadera.leadera.dto.CrearLeadRequest;
import com.leadera.leadera.dto.LeadDetalleResponse;
import com.leadera.leadera.dto.LeadResponseDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Inmobiliaria;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.exception.DuplicateResourceException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AgenteContextResolver agenteContextResolver;

    @Mock
    private InteraccionRepository interaccionRepository;

    @Mock
    private OperacionRepository operacionRepository;

    @Mock
    private CacheManager cacheManager;

    private final ZoneId zonaHoraria = ZoneId.of("America/Argentina/Buenos_Aires");

    private LeadService leadService;

    private static final String EMAIL_AGENTE = "agente@leadera.com";
    private static final Long INMOBILIARIA_ID = 1L;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(
                leadRepository,
                interaccionRepository,
                operacionRepository,
                agenteContextResolver,
                cacheManager,
                zonaHoraria
        );
    }

    // ---------- crearLead ----------

    @Test
    void crearLead_cuandoAgenteNoExiste_lanzaResourceNotFoundException() {
        CrearLeadRequest request = nuevoRequest();
        when(agenteContextResolver.resolverActor(EMAIL_AGENTE))
                .thenThrow(new ResourceNotFoundException("Agente no encontrado"));

        assertThatThrownBy(() -> leadService.crearLead(request, EMAIL_AGENTE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Agente no encontrado");

        verify(leadRepository, never()).save(any());
    }

    @Test
    void crearLead_cuandoEmailYaExisteEnLaInmobiliaria_lanzaDuplicateResourceException() {
        CrearLeadRequest request = nuevoRequest();
        Agente agente = nuevoAgente();
        stubPropietario(agente);
        when(leadRepository.findFirstByAgente_Inmobiliaria_IdAndTelefono(INMOBILIARIA_ID, request.getTelefono()))
                .thenReturn(Optional.empty());
        when(leadRepository.findFirstByAgente_Inmobiliaria_IdAndEmail(INMOBILIARIA_ID, request.getEmail()))
                .thenReturn(Optional.of(nuevoLead()));

        assertThatThrownBy(() -> leadService.crearLead(request, EMAIL_AGENTE))
                .isInstanceOf(DuplicateResourceException.class);

        verify(leadRepository, never()).save(any());
    }

    @Test
    void crearLead_cuandoTelefonoLoTieneOtroAgenteDelEquipo_elMensajeLoNombra() {
        CrearLeadRequest request = nuevoRequest();
        Agente agente = nuevoAgente();

        Agente companero = nuevoAgente();
        companero.setId(8L);
        companero.setNombre("Carla");
        companero.setApellido("Gómez");
        Lead leadDeCompanero = nuevoLead();
        leadDeCompanero.setAgente(companero);

        stubPropietario(agente);
        when(leadRepository.findFirstByAgente_Inmobiliaria_IdAndTelefono(INMOBILIARIA_ID, request.getTelefono()))
                .thenReturn(Optional.of(leadDeCompanero));

        assertThatThrownBy(() -> leadService.crearLead(request, EMAIL_AGENTE))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Carla Gómez");

        verify(leadRepository, never()).save(any());
    }

    @Test
    void crearLead_cuandoDatosValidos_guardaYRetornaConAgenteAsignado() {
        CrearLeadRequest request = nuevoRequest();
        Agente agente = nuevoAgente();

        stubPropietario(agente);
        stubSinDuplicados(request);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(99L);
            return l;
        });

        LeadResponseDTO resultado = leadService.crearLead(request, EMAIL_AGENTE);

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(captor.capture());
        Lead guardado = captor.getValue();

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(99L);
        assertThat(resultado.getNombre()).isEqualTo("Juan");
        assertThat(guardado.getAgente()).isSameAs(agente);
        assertThat(guardado.getEstado()).isEqualTo(EstadoLead.TIBIO);
    }

    @Test
    void crearLead_cuandoFechaEntradaEsNull_laSeteaAutomaticamente() {
        CrearLeadRequest request = nuevoRequest();
        stubPropietario(nuevoAgente());
        stubSinDuplicados(request);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        leadService.crearLead(request, EMAIL_AGENTE);

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(captor.capture());
        assertThat(captor.getValue().getFechaEntrada()).isNotNull();
    }

    @Test
    void crearLead_cuandoEstadoEsNull_aplicaDefaultFrio() {
        CrearLeadRequest request = nuevoRequest();
        request.setEstado(null);

        stubPropietario(nuevoAgente());
        stubSinDuplicados(request);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        leadService.crearLead(request, EMAIL_AGENTE);

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLead.FRIO);
    }

    // ---------- obtenerLeadsPorId ----------

    @Test
    void obtenerLeadsPorId_cuandoLeadNoExiste_lanzaResourceNotFoundException() {
        when(leadRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.obtenerLeadsPorId(1L, EMAIL_AGENTE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe el lead con el id: 1");
    }

    @Test
    void obtenerLeadsPorId_cuandoPerteneceAOtroAgente_lanzaUnauthorizedActionException() {
        Lead lead = nuevoLead();
        Agente otro = nuevoAgente();
        otro.setId(99L);
        otro.setEmail("otro@leadera.com");
        lead.setAgente(otro);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        stubPropietarioPorEmail(nuevoAgente());

        assertThatThrownBy(() -> leadService.obtenerLeadsPorId(1L, EMAIL_AGENTE))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void obtenerLeadsPorId_cuandoLeadPerteneceAlAgente_loRetorna() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        stubPropietarioPorEmail(nuevoAgente());

        LeadDetalleResponse detalle = leadService.obtenerLeadsPorId(1L, EMAIL_AGENTE);

        assertThat(detalle).isNotNull();
        assertThat(detalle.getId()).isEqualTo(1L);
        assertThat(detalle.getNombre()).isEqualTo("Juan");
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_cuandoLeadNoExiste_lanzaResourceNotFoundException() {
        when(leadRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.cambiarEstado(1L, EstadoLead.CALIENTE, EMAIL_AGENTE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(leadRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_cuandoEstadoEsValido_loActualizaYGuarda() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        stubPropietarioPorEmail(nuevoAgente());

        Lead resultado = leadService.cambiarEstado(1L, EstadoLead.CALIENTE, EMAIL_AGENTE);

        assertThat(resultado.getEstado()).isEqualTo(EstadoLead.CALIENTE);
        verify(leadRepository, times(1)).save(lead);
    }

    @Test
    void cambiarEstado_cuandoPerteneceAOtroAgente_lanzaUnauthorizedActionException() {
        Lead lead = nuevoLead();
        Agente otro = nuevoAgente();
        otro.setId(99L);
        otro.setEmail("otro@leadera.com");
        lead.setAgente(otro);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        stubPropietarioPorEmail(nuevoAgente());

        assertThatThrownBy(() -> leadService.cambiarEstado(1L, EstadoLead.CALIENTE, EMAIL_AGENTE))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(leadRepository, never()).save(any());
    }

    // ---------- helpers ----------

    // crearLead resuelve actor -> propietario; sin asistentes en juego, ambos son el mismo agente.
    private void stubPropietario(Agente agente) {
        when(agenteContextResolver.resolverActor(EMAIL_AGENTE)).thenReturn(agente);
        when(agenteContextResolver.resolverPropietario(agente)).thenReturn(agente);
    }

    // Para los checks de ownership (esPropietario) que pasan por resolverPropietarioPorEmail.
    private void stubPropietarioPorEmail(Agente propietario) {
        when(agenteContextResolver.resolverPropietarioPorEmail(EMAIL_AGENTE)).thenReturn(propietario);
    }

    private void stubSinDuplicados(CrearLeadRequest request) {
        when(leadRepository.findFirstByAgente_Inmobiliaria_IdAndTelefono(INMOBILIARIA_ID, request.getTelefono()))
                .thenReturn(Optional.empty());
        when(leadRepository.findFirstByAgente_Inmobiliaria_IdAndEmail(INMOBILIARIA_ID, request.getEmail()))
                .thenReturn(Optional.empty());
    }

    private CrearLeadRequest nuevoRequest() {
        CrearLeadRequest req = new CrearLeadRequest();
        req.setNombre("Juan");
        req.setApellido("Pérez");
        req.setTelefono("1122334455");
        req.setEmail("juan@example.com");
        req.setEstado(EstadoLead.TIBIO);
        req.setOrigen("ZonaProp");
        req.setDescripcionInicial("Interesado en 2 ambientes en Palermo");
        return req;
    }

    private Agente nuevoAgente() {
        Agente a = new Agente();
        a.setId(7L);
        a.setEmail(EMAIL_AGENTE);
        a.setNombre("Mateo");
        a.setApellido("Tester");
        Inmobiliaria inmobiliaria = new Inmobiliaria();
        inmobiliaria.setId(INMOBILIARIA_ID);
        inmobiliaria.setNombre("Inmobiliaria Test");
        a.setInmobiliaria(inmobiliaria);
        return a;
    }

    private Lead nuevoLead() {
        Lead l = new Lead();
        l.setId(1L);
        l.setNombre("Juan");
        l.setApellido("Pérez");
        l.setTelefono("1122334455");
        l.setEmail("juan@example.com");
        l.setEstado(EstadoLead.TIBIO);
        l.setAgente(nuevoAgente());
        return l;
    }
}
