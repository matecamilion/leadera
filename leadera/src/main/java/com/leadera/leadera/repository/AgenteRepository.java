package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Agente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgenteRepository extends JpaRepository<Agente, Long> {


    Optional<Agente> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Agente> findByInmobiliariaIdOrderByIdAsc(Long inmobiliariaId);

    // Asistentes de un supervisor (su jerarquía directa). Por invariante de Fase 0
    // todos pertenecen a la misma inmobiliaria que el supervisor.
    List<Agente> findBySupervisorId(Long supervisorId);
}
