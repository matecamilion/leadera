package com.leadera.leadera.repository;

import com.leadera.leadera.model.EventoPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPropiedadRepository extends JpaRepository<EventoPropiedad, Long> {
    long countByPropiedadId(Long propiedadId);
}
