package com.leadera.leadera.repository;

import com.leadera.leadera.entity.FotoPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FotoPropiedadRepository extends JpaRepository<FotoPropiedad, Long> {

    List<FotoPropiedad> findByPropiedadIdOrderByOrdenAsc(Long propiedadId);

    long countByPropiedadId(Long propiedadId);

    Optional<FotoPropiedad> findByIdAndPropiedadId(Long id, Long propiedadId);
}
