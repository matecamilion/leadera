package com.leadera.leadera.service;


import com.leadera.leadera.dto.CrearInteraccionRequest;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InteraccionService {
    private final InteraccionRepository interaccionRepository;
    private final LeadRepository leadRepository;

    public InteraccionService(InteraccionRepository interaccionRepository,  LeadRepository leadRepository) {
        this.interaccionRepository = interaccionRepository;
        this.leadRepository = leadRepository;
    }

    public Interaccion crearInteraccion(Long leadId, CrearInteraccionRequest request, String emailAgente) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con el id: " + leadId));

        if (!lead.getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para registrar interacciones en este lead.");
        }

        Interaccion interaccion = new Interaccion();
        interaccion.setTipoInteraccion(request.getTipoInteraccion());
        interaccion.setDetalle(request.getDetalle());
        interaccion.setFechaInteraccion(
                request.getFechaInteraccion() != null ? request.getFechaInteraccion() : LocalDateTime.now()
        );
        interaccion.setLead(lead);

        // 3. ACTUALIZAMOS EL LEAD
        lead.setUltimoContacto(interaccion.getFechaInteraccion());

        if (request.getProximoContacto() != null) {
            // Si el agente eligió fecha, se respeta
            lead.setFechaProximoSeguimiento(request.getProximoContacto());
        } else {
            // SI ES NULL: Le damos 3 días para que vuelva a aparecer en la agenda
            lead.setFechaProximoSeguimiento(LocalDateTime.now().plusDays(3));
        }

        leadRepository.save(lead);
        return interaccionRepository.save(interaccion);
    }


}
