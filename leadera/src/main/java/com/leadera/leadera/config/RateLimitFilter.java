package com.leadera.leadera.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Config de rate limit por path: capacidad de requests y ventana de tiempo.
    private record BucketConfig(int capacidad, Duration ventana) {
    }

    // Cada endpoint sensible tiene su propio límite.
    private static final Map<String, BucketConfig> CONFIGS = Map.of(
            "/auth/login", new BucketConfig(10, Duration.ofMinutes(1)),
            "/auth/register", new BucketConfig(5, Duration.ofMinutes(10)),
            // Valida la password actual contra BCrypt: mismo riesgo de fuerza
            // bruta que el login, mismo límite.
            "/auth/cambiar-password", new BucketConfig(10, Duration.ofMinutes(1))
    );

    // path -> (ip -> bucket): cada path lleva sus propios buckets por IP.
    private final Map<String, Map<String, Bucket>> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!aplicaA(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        BucketConfig config = CONFIGS.get(path);
        String ip = obtenerIp(request);
        Bucket bucket = buckets
                .computeIfAbsent(path, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(ip, k -> nuevoBucket(config));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429); // 429 Too Many Requests (no expuesto como constante en jakarta.servlet)
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Demasiados intentos. Por favor esperá unos minutos.\"}");
    }

    private boolean aplicaA(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && CONFIGS.containsKey(request.getRequestURI());
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Tomamos el primer hop por convención de proxies en cadena
            int coma = forwarded.indexOf(',');
            return (coma > 0 ? forwarded.substring(0, coma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket nuevoBucket(BucketConfig config) {
        Bandwidth limite = Bandwidth.classic(
                config.capacidad(),
                Refill.intervally(config.capacidad(), config.ventana()));
        return Bucket.builder().addLimit(limite).build();
    }
}
