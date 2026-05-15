package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Interaccion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InteraccionRepository extends JpaRepository<Interaccion, Long> {
    // Interacciones de los últimos 7 días para los leads del agente
    @Query("SELECT COUNT(i) FROM Interaccion i " +
            "WHERE i.lead.agente.id = :agenteId " +
            "AND i.fechaInteraccion >= :desde")
    long countInteraccionesDesde(@Param("agenteId") Long agenteId,
                                 @Param("desde") LocalDateTime desde);

    // Primera interacción de cada lead del agente (para tiempo de respuesta)
    @Query("SELECT MIN(i.fechaInteraccion) FROM Interaccion i " +
            "WHERE i.lead.id = :leadId")
    LocalDateTime findPrimeraInteraccion(@Param("leadId") Long leadId);

    // Últimas interacciones del agente, ordenadas por fecha desc
    @Query("SELECT i FROM Interaccion i WHERE i.lead.agente.id = :agenteId ORDER BY i.fechaInteraccion DESC")
    List<Interaccion> findUltimasInteracciones(@Param("agenteId") Long agenteId, Pageable pageable);
}
