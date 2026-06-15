package com.leadera.leadera.dto;

import java.util.List;

public record MatchDelDiaDTO(
        Long propiedadId,
        String direccion,
        String zona,
        String tipoVivienda,
        List<CompradorPotencialDTO> compradores
) {}
