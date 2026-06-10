package com.leadera.leadera.service;

import com.leadera.leadera.entity.Agente;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    //Generar el token para un agente especifico
    public String generarToken(UserDetails userDetails, Long id) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("id", id);

        // Claims nuevos del multi-tenant. Son opcionales al leer: los tokens
        // emitidos antes de este cambio siguen siendo válidos (la validación
        // solo mira subject y expiración).
        if (userDetails instanceof Agente agente) {
            if (agente.getRol() != null) {
                claims.put("rol", agente.getRol().name());
            }
            if (agente.getInmobiliaria() != null) {
                claims.put("inmobiliariaId", agente.getInmobiliaria().getId());
            }
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 1. Validar si el token es del usuario y no expiró
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // 2. Chequear si el token ya venció
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 3. Extraer la fecha de vencimiento
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}
