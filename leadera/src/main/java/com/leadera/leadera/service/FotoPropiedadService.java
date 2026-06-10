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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FotoPropiedadService {

    private static final int MAX_FOTOS_POR_PROPIEDAD = 10;
    private static final long MAX_BYTES_POR_FOTO = 5 * 1024 * 1024;

    private final FotoPropiedadRepository fotoRepository;
    private final PropiedadRepository propiedadRepository;
    private final SupabaseStorageService storageService;
    private final ZoneId zonaHoraria;

    public FotoPropiedadService(FotoPropiedadRepository fotoRepository,
                                PropiedadRepository propiedadRepository,
                                SupabaseStorageService storageService,
                                ZoneId zonaHoraria) {
        this.fotoRepository = fotoRepository;
        this.propiedadRepository = propiedadRepository;
        this.storageService = storageService;
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

    public FotoPropiedadDTO subirFoto(Long propiedadId, MultipartFile file, Integer orden, String emailAgente) {
        Propiedad propiedad = obtenerPropiedadDelAgente(propiedadId, emailAgente);

        if (fotoRepository.countByPropiedadId(propiedadId) >= MAX_FOTOS_POR_PROPIEDAD) {
            throw new BadRequestException("Máximo 10 fotos por propiedad");
        }

        // Espejo de las validaciones del frontend: acá son obligatorias, allá UX.
        if (file.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imágenes");
        }
        if (file.getSize() > MAX_BYTES_POR_FOTO) {
            throw new BadRequestException("El archivo supera el máximo de 5 MB");
        }

        String safeName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "foto";
        String path = propiedadId + "/" + System.currentTimeMillis() + "-" + safeName;

        String url = storageService.subirArchivo(file, path);

        FotoPropiedad foto = new FotoPropiedad();
        foto.setPropiedad(propiedad);
        foto.setUrl(url);
        foto.setOrden(orden != null ? orden : 0);
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

        // Primero el objeto de Storage, después la fila: no quedan huérfanos.
        String path = storageService.extraerPath(foto.getUrl());
        if (path != null) {
            storageService.eliminarArchivo(path);
        }

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
