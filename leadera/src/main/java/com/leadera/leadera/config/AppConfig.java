package com.leadera.leadera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    public ZoneId zonaHoraria() {
        return ZoneId.of("America/Argentina/Buenos_Aires");
    }
}
