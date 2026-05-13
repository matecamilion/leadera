package com.leadera.leadera.config;

import com.leadera.leadera.enums.EstadoOperacion;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToEstadoOperacionConverter implements Converter<String, EstadoOperacion> {

    @Override
    public EstadoOperacion convert(String source) {
        return EstadoOperacion.valueOf(source.trim().toUpperCase());
    }
}
