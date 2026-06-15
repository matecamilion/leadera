package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {
    List<Propiedad> findByLeadId(Long leadId);

    @Query("SELECT p FROM Propiedad p " +
            "JOIN FETCH p.lead l " +
            "JOIN l.agente a " +
            "WHERE a.email = :email")
    List<Propiedad> findByAgenteEmail(@Param("email") String email);

    List<Propiedad> findByLeadAgenteEmailAndEstado(String email, EstadoPropiedad estado);
}
