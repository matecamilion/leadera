package com.leadera.leadera.service;

import com.leadera.leadera.model.*;
import com.leadera.leadera.repository.BusquedaRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OperacionService {

    private final OperacionRepository operacionRepository;
    private final LeadRepository leadRepository;
    private final PropiedadRepository propiedadRepository;
    private final BusquedaRepository busquedaRepository;

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public OperacionService(
            OperacionRepository operacionRepository,
            LeadRepository leadRepository,
            PropiedadRepository propiedadRepository,
            BusquedaRepository busquedaRepository
    ) {
        this.operacionRepository = operacionRepository;
        this.leadRepository = leadRepository;
        this.propiedadRepository = propiedadRepository;
        this.busquedaRepository = busquedaRepository;
    }

    public Operacion crearOperacion(Long leadId, Operacion operacionRequest, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("No existe el lead con id: " + leadId));

        if (lead.getAgente() == null || !lead.getAgente().getEmail().equals(emailAgente)) {
            throw new RuntimeException("No tenés permiso para crear operaciones en este lead");
        }

        Operacion operacion = new Operacion();

        operacion.setLead(lead);
        operacion.setAgente(lead.getAgente());
        operacion.setTitulo(operacionRequest.getTitulo());
        operacion.setTipoOperacion(operacionRequest.getTipoOperacion());
        operacion.setDescripcion(operacionRequest.getDescripcion());
        operacion.setEstadoOperacion(EstadoOperacion.ABIERTA);
        operacion.setFechaCreacion(LocalDateTime.now(ZONA_ARGENTINA));

        if (operacionRequest.getTipoOperacion() == TipoOperacion.VENTA) {
            asociarPropiedadAVenta(operacion, operacionRequest, leadId);
        }

        if (operacionRequest.getTipoOperacion() == TipoOperacion.COMPRA) {
            asociarBusquedaACompra(operacion, operacionRequest);
        }

        return operacionRepository.save(operacion);
    }

    private void asociarPropiedadAVenta(Operacion operacion, Operacion operacionRequest, Long leadId) {
        if (operacionRequest.getPropiedad() == null || operacionRequest.getPropiedad().getId() == 0) {
            throw new RuntimeException("Una operación de venta debe tener una propiedad asociada");
        }

        Long propiedadId = operacionRequest.getPropiedad().getId();

        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("No existe la propiedad con id: " + propiedadId));

        if (propiedad.getLead() == null || !propiedad.getLead().getId().equals(leadId)) {
            throw new RuntimeException("La propiedad no pertenece a este lead");
        }

        operacion.setPropiedad(propiedad);
        operacion.setBusqueda(null);
    }

    private void asociarBusquedaACompra(Operacion operacion, Operacion operacionRequest) {
        if (operacionRequest.getBusqueda() == null) {
            throw new RuntimeException("Una operación de compra debe tener una búsqueda asociada");
        }

        Busqueda busqueda = operacionRequest.getBusqueda();
        busqueda.setId(null);
        Busqueda busquedaGuardada = busquedaRepository.save(busqueda);

        operacion.setBusqueda(busquedaGuardada);
        operacion.setPropiedad(null);
    }

    public List<Operacion> obtenerOperacionesDelLead(Long leadId, String emailAgente) {
        return operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailAgente);
    }

    public List<Operacion> obtenerOperacionesAbiertasDelLead(Long leadId, String emailAgente) {
        return operacionRepository.findByLeadIdAndAgenteEmailAndEstadoOperacionNotIn(
                leadId,
                emailAgente,
                List.of(
                        EstadoOperacion.CERRADA_GANADA,
                        EstadoOperacion.CANCELADA
                )
        );
    }

    public Operacion obtenerOperacionPorId(Long leadId, Long operacionId, String emailAgente) {
        return operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailAgente)
                .orElseThrow(() -> new RuntimeException("No existe la operación o no tenés permiso para verla"));
    }


    public Operacion cambiarEstadoOperacion(
            Long leadId,
            Long operacionId,
            EstadoOperacion nuevoEstado,
            String emailAgente
    ) {
        Operacion operacion = operacionRepository.findByIdAndLeadIdAndAgenteEmail(
                operacionId,
                leadId,
                emailAgente
        ).orElseThrow(() -> new RuntimeException("No existe la operación o no tenés permiso para modificarla"));

        if (nuevoEstado == null) {
            throw new RuntimeException("El estado de la operación no puede ser nulo");
        }

        operacion.setEstadoOperacion(nuevoEstado);

        if (
                nuevoEstado == EstadoOperacion.CERRADA_GANADA ||
                        nuevoEstado == EstadoOperacion.CANCELADA
        ) {
            operacion.setFechaCierre(LocalDateTime.now(ZONA_ARGENTINA));
        } else {
            operacion.setFechaCierre(null);
        }

        return operacionRepository.save(operacion);
    }
}