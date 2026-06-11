package com.leadera.leadera.service;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fuente ÚNICA de la regla de "propietario efectivo" de la jerarquía
 * DUENO -> AGENTE -> ASISTENTE.
 *
 * - actor       = el agente real que está logueado (el que dispara la acción).
 * - propietario = de quién SON los leads/interacciones que el actor puede
 *                 leer/operar. Si el actor es ASISTENTE (tiene supervisor),
 *                 es su supervisor; si no, es él mismo.
 *
 * El supervisor siempre pertenece a la misma inmobiliaria que el asistente
 * (invariante garantizada al crear el asistente en Fase 0), por lo que esta
 * regla nunca cruza de inmobiliaria.
 *
 * Nadie más debe reimplementar el check supervisor-o-uno-mismo: todo el resto
 * USA estos métodos.
 */
@Service
@RequiredArgsConstructor
public class AgenteContextResolver {

    private final AgenteRepository agenteRepository;

    /** El agente real detrás del email del token, o 404 si no existe. */
    public Agente resolverActor(String email) {
        return agenteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));
    }

    /** Propietario efectivo del actor: su supervisor si lo tiene, o él mismo. */
    public Agente resolverPropietario(Agente actor) {
        return actor.getSupervisor() != null ? actor.getSupervisor() : actor;
    }

    /** Atajo de composición: propietario efectivo a partir del email del token. */
    public Agente resolverPropietarioPorEmail(String email) {
        return resolverPropietario(resolverActor(email));
    }
}
