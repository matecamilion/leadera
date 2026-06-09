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

    // Primera interacción de TODOS los leads de un agente en una sola query (evita N+1 en el dashboard).
    // Devuelve filas [leadId (Long), primeraFecha (LocalDateTime)].
    @Query("SELECT i.lead.id, MIN(i.fechaInteraccion) FROM Interaccion i " +
            "WHERE i.lead.agente.id = :agenteId GROUP BY i.lead.id")
    List<Object[]> findPrimerasInteraccionesPorAgente(@Param("agenteId") Long agenteId);

    // Últimas interacciones del agente, ordenadas por fecha desc
    @Query("SELECT i FROM Interaccion i WHERE i.lead.agente.id = :agenteId ORDER BY i.fechaInteraccion DESC")
    List<Interaccion> findUltimasInteracciones(@Param("agenteId") Long agenteId, Pageable pageable);

    // Interacciones en un rango cerrado-abierto [desde, hasta)
    @Query("SELECT COUNT(i) FROM Interaccion i " +
            "WHERE i.lead.agente.id = :agenteId " +
            "AND i.fechaInteraccion >= :desde " +
            "AND i.fechaInteraccion < :hasta")
    long countInteraccionesEnRango(@Param("agenteId") Long agenteId,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);
}
