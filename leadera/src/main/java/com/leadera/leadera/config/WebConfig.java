package com.leadera.leadera.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StringToEstadoOperacionConverter stringToEstadoOperacionConverter;

    @Autowired
    public WebConfig(StringToEstadoOperacionConverter stringToEstadoOperacionConverter) {
        this.stringToEstadoOperacionConverter = stringToEstadoOperacionConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToEstadoOperacionConverter);
    }
}
