package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.enums.EstadoPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {
    List<Propiedad> findByLeadId(Long leadId);

    @Query("SELECT p FROM Propiedad p " +
            "JOIN FETCH p.lead l " +
            "JOIN l.agente a " +
            "WHERE a.email = :email")
    List<Propiedad> findByAgenteEmail(@Param("email") String email);

    List<Propiedad> findByLeadAgenteEmailAndEstado(String email, EstadoPropiedad estado);

    @Query("SELECT COUNT(p) FROM Propiedad p JOIN p.lead l JOIN l.agente a " +
            "WHERE a.id = :agenteId AND p.estado = com.leadera.leadera.enums.EstadoPropiedad.DISPONIBLE")
    long countDisponiblesPorAgente(@Param("agenteId") Long agenteId);

    @Query("SELECT COALESCE(SUM(p.precio), 0) FROM Propiedad p JOIN p.lead l JOIN l.agente a " +
            "WHERE a.id = :agenteId AND p.estado = com.leadera.leadera.enums.EstadoPropiedad.DISPONIBLE")
    BigDecimal sumarValorDisponiblesPorAgente(@Param("agenteId") Long agenteId);
}
