package com.leadera.leadera.service;

import com.leadera.leadera.dto.CrearInteraccionRequest;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.TipoInteraccion;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteraccionServiceTest {

    @Mock
    private InteraccionRepository interaccionRepository;

    @Mock
    private LeadRepository leadRepository;

    private final ZoneId zonaHoraria = ZoneId.of("America/Argentina/Buenos_Aires");

    private InteraccionService interaccionService;

    private static final String EMAIL_AGENTE = "agente@leadera.com";

    @BeforeEach
    void setUp() {
        interaccionService = new InteraccionService(interaccionRepository, leadRepository, zonaHoraria);
    }

    @Test
    void crearInteraccion_cuandoLeadNoExiste_lanzaResourceNotFoundException() {
        when(leadRepository.findById(1L)).thenReturn(Optional.empty());

        CrearInteraccionRequest req = nuevoRequest();

        assertThatThrownBy(() -> interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe el lead con el id: 1");

        verify(interaccionRepository, never()).save(any());
    }

    @Test
    void crearInteraccion_cuandoLeadPerteneceAOtroAgente_lanzaUnauthorizedActionException() {
        Lead lead = nuevoLead();
        Agente otro = nuevoAgente();
        otro.setEmail("otro@leadera.com");
        lead.setAgente(otro);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));

        CrearInteraccionRequest req = nuevoRequest();

        assertThatThrownBy(() -> interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(interaccionRepository, never()).save(any());
    }

    @Test
    void crearInteraccion_cuandoFechaInteraccionEsNull_laSeteaConNow() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(interaccionRepository.save(any(Interaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearInteraccionRequest req = nuevoRequest();
        req.setFechaInteraccion(null);

        LocalDateTime antes = LocalDateTime.now(zonaHoraria).minusSeconds(1);
        interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE);
        LocalDateTime despues = LocalDateTime.now(zonaHoraria).plusSeconds(1);

        ArgumentCaptor<Interaccion> captor = ArgumentCaptor.forClass(Interaccion.class);
        verify(interaccionRepository).save(captor.capture());
        LocalDateTime fecha = captor.getValue().getFechaInteraccion();
        assertThat(fecha).isNotNull();
        assertThat(fecha).isAfterOrEqualTo(antes).isBeforeOrEqualTo(despues);
    }

    @Test
    void crearInteraccion_cuandoTipoInteraccionEsNull_aplicaDefaultLlamada() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(interaccionRepository.save(any(Interaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearInteraccionRequest req = nuevoRequest();
        req.setTipoInteraccion(null);

        interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE);

        ArgumentCaptor<Interaccion> captor = ArgumentCaptor.forClass(Interaccion.class);
        verify(interaccionRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoInteraccion()).isEqualTo(TipoInteraccion.LLAMADA);
    }

    @Test
    void crearInteraccion_cuandoProximoContactoEsNull_seteaFechaProximoSeguimientoATresDias() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(interaccionRepository.save(any(Interaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearInteraccionRequest req = nuevoRequest();
        req.setProximoContacto(null);

        LocalDateTime esperadoMin = LocalDateTime.now(zonaHoraria).plusDays(3).minusSeconds(2);
        interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE);
        LocalDateTime esperadoMax = LocalDateTime.now(zonaHoraria).plusDays(3).plusSeconds(2);

        assertThat(lead.getFechaProximoSeguimiento())
                .isNotNull()
                .isAfterOrEqualTo(esperadoMin)
                .isBeforeOrEqualTo(esperadoMax);
    }

    @Test
    void crearInteraccion_cuandoProximoContactoTieneValor_loRespetaExactamente() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(interaccionRepository.save(any(Interaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime esperado = LocalDateTime.of(2026, 6, 15, 10, 30);
        CrearInteraccionRequest req = nuevoRequest();
        req.setProximoContacto(esperado);

        interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE);

        assertThat(lead.getFechaProximoSeguimiento()).isEqualTo(esperado);
    }

    @Test
    void crearInteraccion_cuandoTodoEsValido_actualizaUltimoContactoYGuarda() {
        Lead lead = nuevoLead();
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(interaccionRepository.save(any(Interaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime fecha = LocalDateTime.of(2026, 5, 18, 14, 0);
        CrearInteraccionRequest req = nuevoRequest();
        req.setFechaInteraccion(fecha);

        Interaccion creada = interaccionService.crearInteraccion(1L, req, EMAIL_AGENTE);

        assertThat(creada).isNotNull();
        assertThat(creada.getLead()).isSameAs(lead);
        assertThat(lead.getUltimoContacto()).isEqualTo(fecha);
        verify(leadRepository).save(lead);
        verify(interaccionRepository).save(any(Interaccion.class));
    }

    // ---------- helpers ----------

    private CrearInteraccionRequest nuevoRequest() {
        CrearInteraccionRequest req = new CrearInteraccionRequest();
        req.setTipoInteraccion(TipoInteraccion.WHATSAPP);
        req.setDetalle("Cliente pregunta por disponibilidad");
        req.setFechaInteraccion(LocalDateTime.now(zonaHoraria).truncatedTo(ChronoUnit.SECONDS));
        return req;
    }

    private Agente nuevoAgente() {
        Agente a = new Agente();
        a.setId(7L);
        a.setEmail(EMAIL_AGENTE);
        return a;
    }

    private Lead nuevoLead() {
        Lead l = new Lead();
        l.setId(1L);
        l.setNombre("Juan");
        l.setApellido("Pérez");
        l.setEstado(EstadoLead.TIBIO);
        l.setAgente(nuevoAgente());
        return l;
    }
}
