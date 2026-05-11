package com.leadera.leadera.repository;

import com.leadera.leadera.entity.EventoPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPropiedadRepository extends JpaRepository<EventoPropiedad, Long> {
    long countByPropiedadId(Long propiedadId);
}
