package com.leadera.leadera.repository;

import com.leadera.leadera.entity.EventoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoOperacionRepository extends JpaRepository<EventoOperacion, Long> {
    List<EventoOperacion> findByOperacionIdOrderByFechaDesc(Long operacionId);

    @Query("SELECT e FROM EventoOperacion e " +
            "JOIN FETCH e.operacion o " +
            "LEFT JOIN FETCH o.lead " +
            "WHERE o.propiedad.id = :propiedadId " +
            "ORDER BY e.fecha DESC")
    List<EventoOperacion> findByPropiedadIdOrderByFechaDesc(@Param("propiedadId") Long propiedadId);
}
