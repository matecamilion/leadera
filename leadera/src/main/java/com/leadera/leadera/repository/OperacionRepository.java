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

    List<Operacion> findByPropiedadId(Long propiedadId);

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

    Optional<Operacion> findByIdAndAgenteEmail(Long operacionId, String email);

    @Query("SELECT DISTINCT o FROM Operacion o " +
            "LEFT JOIN FETCH o.lead " +
            "LEFT JOIN FETCH o.propiedad " +
            "LEFT JOIN FETCH o.busqueda " +
            "WHERE o.agente.email = :email " +
            "AND o.estadoOperacion NOT IN ('CERRADA_GANADA', 'CANCELADA')")
    List<Operacion> findPipelineByAgenteEmail(@Param("email") String email);

    @Query("SELECT DISTINCT o FROM Operacion o " +
            "LEFT JOIN FETCH o.lead " +
            "LEFT JOIN FETCH o.propiedad " +
            "LEFT JOIN FETCH o.busqueda " +
            "WHERE o.agente.email = :email " +
            "AND o.estadoOperacion IN ('CERRADA_GANADA', 'CANCELADA') " +
            "ORDER BY o.fechaCierre DESC")
    List<Operacion> findCerradasByAgenteEmail(@Param("email") String email);

    @Query("SELECT COUNT(o) FROM Operacion o WHERE o.lead.id = :leadId AND o.tipoOperacion = :tipo")
    long countByLeadIdAndTipo(@Param("leadId") Long leadId, @Param("tipo") TipoOperacion tipo);

    // Conteos de operaciones por tipo para varios leads en una sola query (evita N+1 en el listado).
    // Devuelve filas [leadId (Long), tipoOperacion (TipoOperacion), count (Long)].
    @Query("SELECT o.lead.id, o.tipoOperacion, COUNT(o) FROM Operacion o " +
            "WHERE o.lead.id IN :leadIds GROUP BY o.lead.id, o.tipoOperacion")
    List<Object[]> countByLeadIdsGroupedByTipo(@Param("leadIds") List<Long> leadIds);

    @Query("SELECT COUNT(o) FROM Operacion o WHERE o.agente.id = :agenteId " +
            "AND o.estadoOperacion = 'CERRADA_GANADA' " +
            "AND MONTH(o.fechaCierre) = MONTH(CURRENT_DATE) " +
            "AND YEAR(o.fechaCierre) = YEAR(CURRENT_DATE)")
    long countOperacionesGanadasDelMes(@Param("agenteId") Long agenteId);

    // ---- Dashboard ----

    // Fechas de cierre en un rango para un estado dado
    @Query("SELECT o.fechaCierre FROM Operacion o " +
            "WHERE o.agente.id = :agenteId " +
            "AND o.estadoOperacion = :estado " +
            "AND o.fechaCierre IS NOT NULL " +
            "AND o.fechaCierre >= :desde " +
            "AND o.fechaCierre < :hasta")
    List<java.time.LocalDateTime> findFechasCierreEnRango(
            @Param("agenteId") Long agenteId,
            @Param("estado") EstadoOperacion estado,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    @Query("SELECT COUNT(o) FROM Operacion o " +
            "WHERE o.agente.id = :agenteId " +
            "AND o.estadoOperacion = :estado " +
            "AND o.fechaCierre IS NOT NULL " +
            "AND o.fechaCierre >= :desde " +
            "AND o.fechaCierre < :hasta")
    long countCierresEnRango(
            @Param("agenteId") Long agenteId,
            @Param("estado") EstadoOperacion estado,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    // Embudo: leads distintos con operación en alguno de los estados dados
    @Query("SELECT COUNT(DISTINCT o.lead.id) FROM Operacion o " +
            "WHERE o.agente.id = :agenteId " +
            "AND o.estadoOperacion IN :estados")
    long countLeadsDistintosConOperacionEnEstados(
            @Param("agenteId") Long agenteId,
            @Param("estados") List<EstadoOperacion> estados);

    // Embudo: leads con al menos un EventoOperacion del tipo dado
    @Query("SELECT COUNT(DISTINCT o.lead.id) FROM EventoOperacion e " +
            "JOIN e.operacion o " +
            "WHERE o.agente.id = :agenteId " +
            "AND e.tipo = com.leadera.leadera.enums.TipoEvento.VISITA")
    long countLeadsConVisita(@Param("agenteId") Long agenteId);

    // Candidatos a matching: operaciones COMPRA del agente con búsqueda definida,
    // lead activo y operación abierta. El filtrado fino (tipoVivienda, zona, precio,
    // ambientes, metros) se hace en Java para evitar issues de mapeo de enums entre
    // Propiedad y Busqueda.
    @Query("SELECT DISTINCT o FROM Operacion o " +
            "JOIN FETCH o.lead l " +
            "JOIN FETCH o.busqueda " +
            "WHERE o.agente.email = :email " +
            "AND o.tipoOperacion = com.leadera.leadera.enums.TipoOperacion.COMPRA " +
            "AND o.busqueda IS NOT NULL " +
            "AND l.estado <> com.leadera.leadera.enums.EstadoLead.INACTIVO " +
            "AND o.estadoOperacion NOT IN (" +
            "  com.leadera.leadera.enums.EstadoOperacion.CERRADA_GANADA, " +
            "  com.leadera.leadera.enums.EstadoOperacion.CANCELADA" +
            ")")
    List<Operacion> findCandidatosParaMatching(@Param("email") String email);
}
