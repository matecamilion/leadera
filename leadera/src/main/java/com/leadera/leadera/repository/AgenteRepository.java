package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Agente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgenteRepository extends JpaRepository<Agente, Long> {


    Optional<Agente> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Agente> findByInmobiliariaIdOrderByIdAsc(Long inmobiliariaId);
}
