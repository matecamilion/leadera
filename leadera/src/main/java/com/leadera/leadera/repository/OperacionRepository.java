package com.leadera.leadera.repository;


import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OperacionRepository extends JpaRepository<Operacion, Long> {
    List<Operacion> findByLeadIdAndAgenteEmail(Long leadId, String email);

    List<Operacion> findByLeadIdAndAgenteEmailAndEstadoOperacionNotIn(
            Long leadId,
            String email,
            List<EstadoOperacion> estadosExcluidos
    );

    Optional<Operacion> findByIdAndLeadIdAndAgenteEmail(
            Long operacionId,
            Long leadId,
            String email
    );

    @Query("SELECT COUNT(o) FROM Operacion o WHERE o.lead.id = :leadId AND o.tipoOperacion = :tipo")
    long countByLeadIdAndTipo(@Param("leadId") Long leadId, @Param("tipo") TipoOperacion tipo);

    @Query("SELECT COUNT(o) FROM Operacion o WHERE o.agente.id = :agenteId " +
            "AND o.estadoOperacion = 'CERRADA_GANADA' " +
            "AND MONTH(o.fechaCierre) = MONTH(CURRENT_DATE) " +
            "AND YEAR(o.fechaCierre) = YEAR(CURRENT_DATE)")
    long countOperacionesGanadasDelMes(@Param("agenteId") Long agenteId);
}
