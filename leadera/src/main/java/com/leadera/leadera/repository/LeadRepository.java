package com.leadera.leadera.repository;

import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Interaccion;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ÍNDICES DB: idx_leads_agente_estado, idx_leads_agente_ultimo_contacto,
//             idx_leads_proximo_seguimiento, idx_leads_fecha_entrada
// Ver: src/main/resources/db/leadera_indexes.sql
public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByEmail(String email);

    // 1. FUNDAMENTALES + FILTRO AGENTE
    List<Lead> findByEstadoAndAgenteEmail(EstadoLead estado, String email);

    // Estos son los que usa tu Dashboard:
    List<Lead> findByUltimoContactoIsNullAndAgenteEmail(String email);

    List<Lead> findByAgenteEmail(String email);

    @Query("""
        SELECT DISTINCT l FROM Lead l
        LEFT JOIN FETCH l.interacciones
        WHERE l.agente.email = :email
        ORDER BY l.fechaEntrada DESC
    """)
    List<Lead> findByAgenteEmailConInteracciones(@Param("email") String email);

    @Query(
        value = """
            SELECT DISTINCT l FROM Lead l
            LEFT JOIN FETCH l.interacciones
            WHERE l.agente.email = :email
        """,
        countQuery = "SELECT COUNT(l) FROM Lead l WHERE l.agente.email = :email"
    )
    Page<Lead> findByAgenteEmailConInteraccionesPaginado(@Param("email") String email, Pageable pageable);

    long countByAgenteIdAndEstadoNot(Long agenteId, EstadoLead estado);

    // 2. PRIORITARIOS + FILTRO AGENTE
    List<Lead> findByEstadoAndUltimoContactoBeforeAndAgenteEmail(EstadoLead estado, LocalDateTime fechaLimite, String email);

    // finDelDia es exclusivo: pasar inicioDeMañana (hoy 00:00 + 1 día) para incluir
    // todo el día actual aunque el seguimiento esté programado para "más tarde hoy".
    @Query("""
    SELECT l FROM Lead l
    WHERE l.fechaProximoSeguimiento IS NOT NULL
    AND l.fechaProximoSeguimiento < :finDelDia
    AND l.estado <> :inactivo
    AND l.agente.email = :email
""")
    List<Lead> findSeguimientosPendientes(
            @Param("finDelDia") LocalDateTime finDelDia,
            @Param("email") String email,
            @Param("inactivo") EstadoLead inactivo
    );

    // 4. OTROS MÉTODOS (Si los usás, agregales el filtro también)
    List<Lead> findByUltimoContactoBeforeAndUltimoContactoIsNotNullAndAgenteEmail(LocalDateTime fecha, String email);


    // Cuenta todos los leads de un agente
    long countByAgenteId(Long agenteId);

    // Cuenta leads por agente y estado específico
    long countByAgenteIdAndEstado(Long agenteId, EstadoLead estado);

    // Leads que ENTRARON este mes (basado en fecha de creación)
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.agente.id = :agenteId " +
            "AND MONTH(l.fechaEntrada) = MONTH(CURRENT_DATE) " +
            "AND YEAR(l.fechaEntrada) = YEAR(CURRENT_DATE)")
    long countIngresosDelMes(@Param("agenteId") Long agenteId);


    // Leads del agente con su fechaEntrada (para calcular tiempo de respuesta)
    @Query("SELECT l FROM Lead l WHERE l.agente.id = :agenteId AND l.fechaEntrada IS NOT NULL")
    List<Lead> findLeadsConFechaEntrada(@Param("agenteId") Long agenteId);

    // ---- Unicidad a nivel inmobiliaria ----
    // Devuelven el lead duplicado (no solo exists) para poder armar el mensaje
    // de error con el nombre del agente del equipo que ya lo tiene.
    Optional<Lead> findFirstByAgente_Inmobiliaria_IdAndTelefono(Long inmobiliariaId, String telefono);
    Optional<Lead> findFirstByAgente_Inmobiliaria_IdAndEmail(Long inmobiliariaId, String email);
    Optional<Lead> findFirstByAgente_Inmobiliaria_IdAndTelefonoAndIdNot(Long inmobiliariaId, String telefono, Long id);
    Optional<Lead> findFirstByAgente_Inmobiliaria_IdAndEmailAndIdNot(Long inmobiliariaId, String email, Long id);

    // ---- Vista de equipo del dueño ----

    // Misma técnica que findByAgenteEmailConInteraccionesPaginado pero a nivel
    // inmobiliaria, con el agente fetcheado para la columna "Agente" del listado.
    @Query(
        value = """
            SELECT DISTINCT l FROM Lead l
            LEFT JOIN FETCH l.interacciones
            JOIN FETCH l.agente
            WHERE l.agente.inmobiliaria.id = :inmobiliariaId
        """,
        countQuery = "SELECT COUNT(l) FROM Lead l WHERE l.agente.inmobiliaria.id = :inmobiliariaId"
    )
    Page<Lead> findByInmobiliariaConInteraccionesPaginado(@Param("inmobiliariaId") Long inmobiliariaId, Pageable pageable);

    // Conteo de leads activos por agente del equipo en una sola query (evita N+1).
    // Devuelve filas [agenteId (Long), count (Long)].
    @Query("SELECT l.agente.id, COUNT(l) FROM Lead l " +
            "WHERE l.agente.inmobiliaria.id = :inmobiliariaId " +
            "AND l.estado <> com.leadera.leadera.enums.EstadoLead.INACTIVO " +
            "GROUP BY l.agente.id")
    List<Object[]> countLeadsActivosPorAgente(@Param("inmobiliariaId") Long inmobiliariaId);


    List<Lead> findByUltimoContactoIsNullAndAgenteEmailAndEstadoNot(String email, EstadoLead estado);

    List<Lead> findByUltimoContactoAfterAndAgenteEmail(LocalDateTime fecha, String email);

    // ---- Dashboard ----

    // Leads ingresados en un rango
    @Query("SELECT l.fechaEntrada FROM Lead l WHERE l.agente.id = :agenteId " +
            "AND l.fechaEntrada >= :desde AND l.fechaEntrada < :hasta")
    List<LocalDateTime> findFechasIngresoEnRango(@Param("agenteId") Long agenteId,
                                                 @Param("desde") LocalDateTime desde,
                                                 @Param("hasta") LocalDateTime hasta);

    long countByAgenteIdAndFechaEntradaGreaterThanEqualAndFechaEntradaLessThan(
            Long agenteId, LocalDateTime desde, LocalDateTime hasta);

    // Origen agrupado en un rango (NULL/vacío -> "Sin origen")
    @Query("SELECT COALESCE(NULLIF(TRIM(l.origen), ''), 'Sin origen') as origen, COUNT(l) " +
            "FROM Lead l WHERE l.agente.id = :agenteId " +
            "AND l.fechaEntrada >= :desde AND l.fechaEntrada < :hasta " +
            "GROUP BY COALESCE(NULLIF(TRIM(l.origen), ''), 'Sin origen') " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> contarOrigenesEnRango(@Param("agenteId") Long agenteId,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    // Embudo: leads con al menos una interacción
    @Query("SELECT COUNT(DISTINCT l) FROM Lead l " +
            "WHERE l.agente.id = :agenteId AND SIZE(l.interacciones) > 0")
    long countContactados(@Param("agenteId") Long agenteId);

    // Embudo: leads calificados (no FRIO ni INACTIVO)
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.agente.id = :agenteId " +
            "AND l.estado <> com.leadera.leadera.enums.EstadoLead.FRIO " +
            "AND l.estado <> com.leadera.leadera.enums.EstadoLead.INACTIVO")
    long countCalificados(@Param("agenteId") Long agenteId);
}
