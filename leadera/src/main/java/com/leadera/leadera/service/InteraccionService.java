package com.leadera.leadera.service;


import com.leadera.leadera.model.Interaccion;
import com.leadera.leadera.model.Lead;
import com.leadera.leadera.model.TipoInteraccion;
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

    public Interaccion crearInteraccion(Long leadId, Interaccion interaccion, LocalDateTime fechaProximoContacto) {
        System.out.println("Fecha próximo contacto recibida: " + fechaProximoContacto);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("No existe el lead con el id: " + leadId));

        interaccion.setLead(lead);
        if(interaccion.getFechaInteraccion() == null) {

            interaccion.setFechaInteraccion(LocalDateTime.now());
        }
        if(interaccion.getTipoInteraccion() == null) {
            interaccion.setTipoInteraccion(TipoInteraccion.LLAMADA);
        }

        // 3. ACTUALIZAMOS EL LEAD
        lead.setUltimoContacto(interaccion.getFechaInteraccion());


        if (fechaProximoContacto != null) {
            // Si el agente eligió fecha, se respeta
            lead.setFechaProximoSeguimiento(fechaProximoContacto);
        } else {
            // SI ES NULL: Le damos 3 días para que vuelva a aparecer en la agenda
            lead.setFechaProximoSeguimiento(LocalDateTime.now().plusDays(3));
        }

        leadRepository.save(lead);
        return interaccionRepository.save(interaccion);
    }


}
