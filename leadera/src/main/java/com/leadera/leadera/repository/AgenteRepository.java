package com.leadera.leadera.repository;

import com.leadera.leadera.model.Agente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgenteRepository extends JpaRepository<Agente, Long> {


    Optional<Agente> findByEmail(String email);
}
