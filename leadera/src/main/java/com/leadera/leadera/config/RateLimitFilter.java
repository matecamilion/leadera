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

    private static final String LOGIN_PATH = "/auth/login";
    private static final int CAPACIDAD = 10;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!aplicaA(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = obtenerIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> nuevoBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429); // 429 Too Many Requests (no expuesto como constante en jakarta.servlet)
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Demasiados intentos. Por favor esperá un minuto.\"}");
    }

    private boolean aplicaA(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
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

    private Bucket nuevoBucket() {
        Bandwidth limite = Bandwidth.classic(CAPACIDAD, Refill.intervally(CAPACIDAD, VENTANA));
        return Bucket.builder().addLimit(limite).build();
    }
}
