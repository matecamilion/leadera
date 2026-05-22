package com.leadera.leadera.dto;

import com.leadera.leadera.entity.FotoPropiedad;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FotoPropiedadDTO {
    private Long id;
    private String url;
    private Integer orden;

    public static FotoPropiedadDTO fromEntity(FotoPropiedad f) {
        return new FotoPropiedadDTO(f.getId(), f.getUrl(), f.getOrden());
    }
}
