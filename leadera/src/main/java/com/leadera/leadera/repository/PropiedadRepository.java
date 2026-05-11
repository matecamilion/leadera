package com.leadera.leadera.repository;

import com.leadera.leadera.model.EstadoPropiedad;
import com.leadera.leadera.model.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {
    List<Propiedad> findByLeadId(Long leadId) ;
}
