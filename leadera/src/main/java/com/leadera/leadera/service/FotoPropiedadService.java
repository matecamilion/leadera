package com.leadera.leadera.service;

import com.leadera.leadera.dto.CrearFotoRequest;
import com.leadera.leadera.dto.FotoPropiedadDTO;
import com.leadera.leadera.entity.FotoPropiedad;
import com.leadera.leadera.entity.Propiedad;
import com.leadera.leadera.exception.BadRequestException;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.exception.UnauthorizedActionException;
import com.leadera.leadera.repository.FotoPropiedadRepository;
import com.leadera.leadera.repository.PropiedadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FotoPropiedadService {

    private static final int MAX_FOTOS_POR_PROPIEDAD = 10;

    private final FotoPropiedadRepository fotoRepository;
    private final PropiedadRepository propiedadRepository;
    private final ZoneId zonaHoraria;

    public FotoPropiedadService(FotoPropiedadRepository fotoRepository,
                                PropiedadRepository propiedadRepository,
                                ZoneId zonaHoraria) {
        this.fotoRepository = fotoRepository;
        this.propiedadRepository = propiedadRepository;
        this.zonaHoraria = zonaHoraria;
    }

    public FotoPropiedadDTO agregarFoto(Long propiedadId, CrearFotoRequest request, String emailAgente) {
        Propiedad propiedad = obtenerPropiedadDelAgente(propiedadId, emailAgente);

        if (fotoRepository.countByPropiedadId(propiedadId) >= MAX_FOTOS_POR_PROPIEDAD) {
            throw new BadRequestException("Máximo 10 fotos por propiedad");
        }

        FotoPropiedad foto = new FotoPropiedad();
        foto.setPropiedad(propiedad);
        foto.setUrl(request.getUrl());
        foto.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        foto.setFechaSubida(LocalDateTime.now(zonaHoraria));

        return FotoPropiedadDTO.fromEntity(fotoRepository.save(foto));
    }

    public List<FotoPropiedadDTO> listarFotos(Long propiedadId, String emailAgente) {
        obtenerPropiedadDelAgente(propiedadId, emailAgente);
        return fotoRepository.findByPropiedadIdOrderByOrdenAsc(propiedadId).stream()
                .map(FotoPropiedadDTO::fromEntity)
                .toList();
    }

    public void eliminarFoto(Long propiedadId, Long fotoId, String emailAgente) {
        obtenerPropiedadDelAgente(propiedadId, emailAgente);
        FotoPropiedad foto = fotoRepository.findByIdAndPropiedadId(fotoId, propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        fotoRepository.delete(foto);
    }

    private Propiedad obtenerPropiedadDelAgente(Long propiedadId, String emailAgente) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        if (propiedad.getLead() == null
                || propiedad.getLead().getAgente() == null
                || !propiedad.getLead().getAgente().getEmail().equals(emailAgente)) {
            throw new UnauthorizedActionException("No tenés permiso para modificar las fotos de esta propiedad.");
        }
        return propiedad;
    }
}
