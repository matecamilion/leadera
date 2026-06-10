package com.leadera.leadera.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Acceso a Supabase Storage con la service role key (variable de entorno,
 * solo backend). El frontend ya no habla con Storage: la anon key dejó de
 * viajar en el bundle y el bucket puede cerrarse para el rol anon.
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final RestTemplate restTemplate;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.bucket}") String bucket) {
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sube un archivo a Supabase Storage usando la service role key.
     * @return URL pública del objeto subido
     */
    public String subirArchivo(MultipartFile file, String path) {
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = headersConServiceRole();
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));
        headers.set("x-upsert", "false");

        try {
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Error al subir archivo a Storage: " + e.getMessage(), e);
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    /**
     * Elimina un objeto de Supabase Storage dado su path relativo.
     * No lanza excepción si el objeto no existe (idempotente): la fila en DB
     * se borra igual y no queda colgada de un objeto inexistente.
     */
    public void eliminarArchivo(String path) {
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE,
                    new HttpEntity<>(headersConServiceRole()), String.class);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el objeto '{}' de Storage: {}", path, e.getMessage());
        }
    }

    /**
     * Extrae el path relativo de una URL pública de Supabase Storage.
     * Ejemplo: "https://xxx.supabase.co/storage/v1/object/public/fotos-propiedades/5/123-foto.jpg"
     * → "5/123-foto.jpg"
     */
    public String extraerPath(String urlPublica) {
        String prefix = supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
        if (urlPublica != null && urlPublica.startsWith(prefix)) {
            return urlPublica.substring(prefix.length());
        }
        return null;
    }

    private HttpHeaders headersConServiceRole() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        return headers;
    }
}
