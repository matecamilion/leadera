# Fase 0 — Seguridad Crítica: Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar las 5 vulnerabilidades de seguridad de Fase 0 antes de que entre cualquier cliente real.

**Architecture:** Cambios aislados sin ruptura de contrato de API. El cambio principal (Task 3-5) reemplaza `@RequestBody Operacion` (entity cruda) por `CrearOperacionRequest` DTO en el POST de operaciones. Los GET responses también pasan de entity a `OperacionDTO`. El contrato JSON sigue siendo compatible con el frontend Angular existente.

**Tech Stack:** Java 17 + Spring Boot 3.3, Angular 20, JUnit 5 + MockMvc, PostgreSQL/H2 (tests)

---

## Task 1: Fix environment.ts — dev apuntando a prod

**Files:**
- Modify: `leadera-front/src/environments/environment.ts`

- [ ] **Step 1: Editar environment.ts**

Reemplazar el contenido de `leadera-front/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
};
```

- [ ] **Step 2: Commit**

```bash
git add leadera-front/src/environments/environment.ts
git commit -m "fix: dev environment apunta a localhost:8080, no a prod"
```

---

## Task 2: Hardening CORS — remover soporte de wildcards en código

**Files:**
- Modify: `leadera/src/main/java/com/leadera/leadera/config/SecurityConfig.java`

**Contexto:** `setAllowedOriginPatterns` acepta wildcards como `leadera-*.vercel.app`. Si la variable de entorno `CORS_ALLOWED_ORIGINS` en Render contiene ese patrón, cualquier subdeploy de Vercel con ese prefijo puede hacer requests autenticados. La fix tiene dos partes: código + Render dashboard.

- [ ] **Step 1: Cambiar de patterns a origins exactos en SecurityConfig**

En `leadera/src/main/java/com/leadera/leadera/config/SecurityConfig.java`, reemplazar el método `corsConfigurationSource()` completo:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

    // setAllowedOrigins rechaza wildcards (a diferencia de setAllowedOriginPatterns).
    // La env var CORS_ALLOWED_ORIGINS debe listar URLs exactas.
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

- [ ] **Step 2: Actualizar Render env var (acción manual en dashboard)**

1. Ir a **Render → tu backend → Environment → `CORS_ALLOWED_ORIGINS`**
2. Cambiar el valor al URL exacto del frontend en producción, sin wildcards. Ejemplo:
   ```
   https://leadera-front.vercel.app,https://tu-dominio-custom.com
   ```
3. Guardar. Render hace redeploy automático.

- [ ] **Step 3: Verificar en dev que CORS sigue funcionando**

Levantar el backend local y hacer una request desde `http://localhost:4200` al backend. El header `Origin: http://localhost:4200` debe ser aceptado (es el default de `CORS_ALLOWED_ORIGINS` en `application-dev.properties`).

- [ ] **Step 4: Commit**

```bash
git add leadera/src/main/java/com/leadera/leadera/config/SecurityConfig.java
git commit -m "security: reemplazar allowedOriginPatterns por allowedOrigins para prevenir wildcards CORS"
```

---

## Task 3: Verificar log PII (JWT filter) — ya resuelto

**Files:**
- Read-only: `leadera/src/main/java/com/leadera/leadera/config/JwtAuthenticationFilter.java`

- [ ] **Step 1: Confirmar que no hay email en logs**

Verificar que `JwtAuthenticationFilter.java` NO loguea el email del usuario. Las líneas relevantes deben ser:

```java
log.debug("Email extraído del token");           // sin el email real
log.debug("Token JWT inválido o vencido: {}", e.getMessage());
log.warn("Token rechazado por validación");       // sin el email real
```

Si alguna línea contiene `log.warn(...email...)` o `log.info(...userEmail...)`, cambiarla a `log.debug` y eliminar el email del mensaje.

- [ ] **Step 2: Commit si hubo cambio (omitir si ya estaba correcto)**

```bash
git add leadera/src/main/java/com/leadera/leadera/config/JwtAuthenticationFilter.java
git commit -m "security: remover email de logs para cumplir Ley 25.326"
```

---

## Task 4: Crear `CrearOperacionRequest.java` DTO

**Files:**
- Create: `leadera/src/main/java/com/leadera/leadera/dto/CrearOperacionRequest.java`

**Contexto:** El frontend ya envía este formato (no envía `id`). La entity `Operacion` no debe llegar al controller; el id nunca debe venir en el body de creación.

- [ ] **Step 1: Crear el archivo**

Crear `leadera/src/main/java/com/leadera/leadera/dto/CrearOperacionRequest.java`:

```java
package com.leadera.leadera.dto;

import com.leadera.leadera.enums.TipoOperacion;
import com.leadera.leadera.enums.TipoVivienda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CrearOperacionRequest(
        @NotBlank String titulo,
        @NotNull TipoOperacion tipoOperacion,
        String descripcion,
        PropiedadRef propiedad,
        BusquedaData busqueda
) {
    // Referencia a propiedad existente por id (solo para VENTA).
    // Jackson ignora cualquier otro campo que venga en el JSON.
    public record PropiedadRef(Long id) {}

    // Datos de búsqueda para operaciones de COMPRA.
    // No tiene campo id: imposibilita pisar una Busqueda existente.
    public record BusquedaData(
            BigDecimal precioMin,
            BigDecimal precioMax,
            Integer cantidadAmbientes,
            Integer metrosTotales,
            Integer metrosCubiertos,
            Integer metrosDescubiertos,
            TipoVivienda tipoVivienda,
            String zona,
            String observaciones
    ) {}
}
```

---

## Task 5: Crear `OperacionDTO.java`

**Files:**
- Create: `leadera/src/main/java/com/leadera/leadera/dto/OperacionDTO.java`

**Contexto:** Reemplaza el retorno de entity cruda `Operacion` en los GET/POST/PATCH del controller. Campos que espera el frontend Angular (`operacion-service.ts` interface `Operacion`): id, titulo, tipoOperacion, estadoOperacion, descripcion, fechaCreacion, fechaCierre, fechaProximoSeguimiento, propiedad, busqueda, leadId, montoOperacion.

- [ ] **Step 1: Crear el archivo**

Crear `leadera/src/main/java/com/leadera/leadera/dto/OperacionDTO.java`:

```java
package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Busqueda;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.TipoOperacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionDTO(
        Long id,
        String titulo,
        TipoOperacion tipoOperacion,
        EstadoOperacion estadoOperacion,
        String descripcion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        LocalDateTime fechaProximoSeguimiento,
        PropiedadDTO propiedad,   // null para operaciones de COMPRA
        Busqueda busqueda,        // null para operaciones de VENTA; entity leaf sin circular refs
        Long leadId,
        BigDecimal montoOperacion
) {}
```

---

## Task 6: Actualizar `OperacionService` para usar los DTOs

**Files:**
- Modify: `leadera/src/main/java/com/leadera/leadera/service/OperacionService.java`

**Cambios:** (1) `crearOperacion` acepta `CrearOperacionRequest` en lugar de `Operacion`; (2) todos los métodos que retornaban `Operacion` o `List<Operacion>` ahora retornan `OperacionDTO` / `List<OperacionDTO>`; (3) helper privado `toDTO`; (4) `asociarPropiedadAVenta` y `asociarBusquedaACompra` trabajan con los records del DTO.

- [ ] **Step 1: Agregar imports necesarios**

Al inicio de `OperacionService.java`, agregar:

```java
import com.leadera.leadera.dto.CrearOperacionRequest;
import com.leadera.leadera.dto.OperacionDTO;
import com.leadera.leadera.mapper.PropiedadMapper;
```

- [ ] **Step 2: Reemplazar `crearOperacion`**

Reemplazar el método completo `crearOperacion` y los privados `asociarPropiedadAVenta` y `asociarBusquedaACompra`:

```java
@Transactional
public OperacionDTO crearOperacion(Long leadId, CrearOperacionRequest request, String emailAgente) {
    Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el lead con id: " + leadId));

    String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
    if (lead.getAgente() == null || !lead.getAgente().getEmail().equals(emailPropietario)) {
        throw new UnauthorizedActionException("No tenés permiso para crear operaciones en este lead");
    }

    Operacion operacion = new Operacion();
    operacion.setLead(lead);
    operacion.setAgente(lead.getAgente());
    operacion.setTitulo(request.titulo());
    operacion.setTipoOperacion(request.tipoOperacion());
    operacion.setDescripcion(request.descripcion());
    operacion.setEstadoOperacion(EstadoOperacion.PUBLICADA);
    operacion.setFechaCreacion(LocalDateTime.now(zonaHoraria));

    if (request.tipoOperacion() == TipoOperacion.VENTA) {
        asociarPropiedadAVenta(operacion, request, leadId);
    }

    if (request.tipoOperacion() == TipoOperacion.COMPRA) {
        asociarBusquedaACompra(operacion, request);
    }

    Operacion guardada = operacionRepository.save(operacion);

    if (guardada.getTipoOperacion() == TipoOperacion.VENTA && guardada.getPropiedad() != null) {
        sincronizarEstadoPropiedad(guardada.getPropiedad());
    }

    return toDTO(guardada);
}

private void asociarPropiedadAVenta(Operacion operacion, CrearOperacionRequest request, Long leadId) {
    if (request.propiedad() == null || request.propiedad().id() == null) {
        throw new BadRequestException("Una operación de venta debe tener una propiedad asociada");
    }

    Long propiedadId = request.propiedad().id();

    Propiedad propiedad = propiedadRepository.findById(propiedadId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la propiedad con id: " + propiedadId));

    if (propiedad.getLead() == null || !propiedad.getLead().getId().equals(leadId)) {
        throw new BadRequestException("La propiedad no pertenece a este lead");
    }

    if (propiedad.getEstado() == EstadoPropiedad.VENDIDA) {
        throw new BadRequestException("No se puede crear una operación de venta sobre una propiedad ya vendida.");
    }

    operacion.setPropiedad(propiedad);
    operacion.setBusqueda(null);
}

private void asociarBusquedaACompra(Operacion operacion, CrearOperacionRequest request) {
    if (request.busqueda() == null) {
        throw new BadRequestException("Una operación de compra debe tener una búsqueda asociada");
    }

    CrearOperacionRequest.BusquedaData data = request.busqueda();
    Busqueda busqueda = new Busqueda();
    busqueda.setPrecioMin(data.precioMin());
    busqueda.setPrecioMax(data.precioMax());
    busqueda.setCantidadAmbientes(data.cantidadAmbientes());
    busqueda.setMetrosTotales(data.metrosTotales());
    busqueda.setMetrosCubiertos(data.metrosCubiertos());
    busqueda.setMetrosDescubiertos(data.metrosDescubiertos());
    busqueda.setTipoVivienda(data.tipoVivienda());
    busqueda.setZona(data.zona());
    busqueda.setObservaciones(data.observaciones());

    operacion.setBusqueda(busquedaRepository.save(busqueda));
    operacion.setPropiedad(null);
}
```

- [ ] **Step 3: Reemplazar métodos GET para retornar `OperacionDTO`**

Reemplazar `obtenerOperacionesDelLead`, `obtenerOperacionesAbiertasDelLead`, `obtenerOperacionPorId` y `cambiarEstadoOperacion`:

```java
public List<OperacionDTO> obtenerOperacionesDelLead(Long leadId, String emailAgente) {
    String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
    return operacionRepository.findByLeadIdAndAgenteEmail(leadId, emailPropietario)
            .stream().map(this::toDTO).collect(Collectors.toList());
}

public List<OperacionDTO> obtenerOperacionesAbiertasDelLead(Long leadId, String emailAgente) {
    String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
    return operacionRepository.findByLeadIdAndAgenteEmailAndEstadoOperacionNotIn(
            leadId,
            emailPropietario,
            List.of(EstadoOperacion.CERRADA_GANADA, EstadoOperacion.CANCELADA)
    ).stream().map(this::toDTO).collect(Collectors.toList());
}

public OperacionDTO obtenerOperacionPorId(Long leadId, Long operacionId, String emailAgente) {
    String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
    return operacionRepository.findByIdAndLeadIdAndAgenteEmail(operacionId, leadId, emailPropietario)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para verla"));
}

@Transactional
public OperacionDTO cambiarEstadoOperacion(
        Long leadId,
        Long operacionId,
        EstadoOperacion nuevoEstado,
        String emailAgente
) {
    String emailPropietario = agenteContextResolver.resolverPropietarioPorEmail(emailAgente).getEmail();
    Operacion operacion = operacionRepository.findByIdAndLeadIdAndAgenteEmail(
            operacionId, leadId, emailPropietario
    ).orElseThrow(() -> new ResourceNotFoundException("No existe la operación o no tenés permiso para modificarla"));

    return toDTO(aplicarCambioDeEstado(operacion, nuevoEstado));
}
```

- [ ] **Step 4: Agregar helper `toDTO` al final de la clase (antes del último `}`)**

```java
private OperacionDTO toDTO(Operacion operacion) {
    return new OperacionDTO(
            operacion.getId(),
            operacion.getTitulo(),
            operacion.getTipoOperacion(),
            operacion.getEstadoOperacion(),
            operacion.getDescripcion(),
            operacion.getFechaCreacion(),
            operacion.getFechaCierre(),
            operacion.getFechaProximoSeguimiento(),
            PropiedadMapper.toDTO(operacion.getPropiedad()),
            operacion.getBusqueda(),
            operacion.getLead() != null ? operacion.getLead().getId() : null,
            operacion.getMontoOperacion()
    );
}
```

---

## Task 7: Actualizar `OperacionController` para usar los DTOs

**Files:**
- Modify: `leadera/src/main/java/com/leadera/leadera/controller/OperacionController.java`

- [ ] **Step 1: Reemplazar el archivo completo**

```java
package com.leadera.leadera.controller;

import com.leadera.leadera.dto.CrearEventoRequest;
import com.leadera.leadera.dto.CrearOperacionRequest;
import com.leadera.leadera.dto.EventoOperacionDTO;
import com.leadera.leadera.dto.OperacionDTO;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.service.OperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leads/{leadId}/operaciones")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping
    public ResponseEntity<OperacionDTO> crearOperacion(
            @PathVariable Long leadId,
            @Valid @RequestBody CrearOperacionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.crearOperacion(leadId, request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<OperacionDTO>> obtenerOperacionesDelLead(
            @PathVariable Long leadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.obtenerOperacionesDelLead(leadId, authentication.getName()));
    }

    @GetMapping("/abiertas")
    public ResponseEntity<List<OperacionDTO>> obtenerOperacionesAbiertasDelLead(
            @PathVariable Long leadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.obtenerOperacionesAbiertasDelLead(leadId, authentication.getName()));
    }

    @GetMapping("/{operacionId}")
    public ResponseEntity<OperacionDTO> obtenerOperacionPorId(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.obtenerOperacionPorId(leadId, operacionId, authentication.getName()));
    }

    @PatchMapping("/{operacionId}/estado")
    public ResponseEntity<OperacionDTO> cambiarEstadoOperacion(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            @RequestParam EstadoOperacion estadoOperacion,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                operacionService.cambiarEstadoOperacion(leadId, operacionId, estadoOperacion, authentication.getName())
        );
    }

    @PostMapping("/{operacionId}/eventos")
    public ResponseEntity<EventoOperacionDTO> registrarEvento(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            @Valid @RequestBody CrearEventoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.registrarEvento(leadId, operacionId, request, authentication.getName()));
    }

    @GetMapping("/{operacionId}/eventos")
    public ResponseEntity<List<EventoOperacionDTO>> listarEventos(
            @PathVariable Long leadId,
            @PathVariable Long operacionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operacionService.obtenerEventos(leadId, operacionId, authentication.getName()));
    }
}
```

---

## Task 8: Agregar test de regresión para crearOperacion

**Files:**
- Modify: `leadera/src/test/java/com/leadera/leadera/integration/CrossTenantWriteTest.java`

- [ ] **Step 1: Agregar el test al final de la clase (antes del último `}`)**

```java
@Test
void crearOperacion_ignora_id_de_busqueda_en_body() throws Exception {
    Lead leadA = crearLead(agenteA, "Comprador", "DeA", "5555555555");
    Operacion operacionA = crearOperacionCompra(agenteA, leadA);
    Long busquedaAId = operacionA.getBusqueda().getId();

    Lead leadB = crearLead(agenteB, "Comprador", "DeB", "6666666666");

    // B manda en el body el id de la busqueda de A intentando pisar datos ajenos.
    // Con BusquedaData (sin campo id), Jackson lo ignora y se crea una busqueda nueva.
    mockMvc.perform(post("/leads/" + leadB.getId() + "/operaciones")
                    .header("Authorization", bearer(agenteB))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "titulo": "Búsqueda de DEPTO",
                              "tipoOperacion": "COMPRA",
                              "descripcion": "test",
                              "busqueda": {
                                "zona": "Palermo",
                                "tipoVivienda": "DEPTO",
                                "id": %d
                              }
                            }
                            """.formatted(busquedaAId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.tipoOperacion").value("COMPRA"))
            .andExpect(jsonPath("$.leadId").value(leadB.getId().intValue()));

    // La busqueda de A sigue intacta.
    assertThat(busquedaRepository.findById(busquedaAId)).isPresent();
    assertThat(busquedaRepository.findById(busquedaAId).get().getZona()).isEqualTo("Palermo");
}

@Test
void crearOperacion_devuelve_OperacionDTO_sin_exponer_entity() throws Exception {
    Lead leadA = crearLead(agenteA, "Comprador", "DeA", "7777777777");

    String response = mockMvc.perform(post("/leads/" + leadA.getId() + "/operaciones")
                    .header("Authorization", bearer(agenteA))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "titulo": "Búsqueda test",
                              "tipoOperacion": "COMPRA",
                              "descripcion": "desc",
                              "busqueda": {
                                "zona": "Microcentro",
                                "tipoVivienda": "DEPTO"
                              }
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.titulo").value("Búsqueda test"))
            .andExpect(jsonPath("$.estadoOperacion").value("PUBLICADA"))
            .andExpect(jsonPath("$.leadId").value(leadA.getId().intValue()))
            .andReturn().getResponse().getContentAsString();

    // La entity no debe exponer el campo "agente" ni "lead" completos.
    assertThat(response).doesNotContain("\"agente\":{");
    assertThat(response).doesNotContain("\"lead\":{");
}
```

- [ ] **Step 2: Correr los tests**

```bash
cd leadera
./mvnw test -pl . -Dtest=CrossTenantWriteTest -q
```

Salida esperada: todos los tests (incluyendo los 2 nuevos) en PASS.

- [ ] **Step 3: Correr la suite completa para verificar no-regresión**

```bash
./mvnw test -q
```

Salida esperada: BUILD SUCCESS, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add \
  leadera/src/main/java/com/leadera/leadera/dto/CrearOperacionRequest.java \
  leadera/src/main/java/com/leadera/leadera/dto/OperacionDTO.java \
  leadera/src/main/java/com/leadera/leadera/service/OperacionService.java \
  leadera/src/main/java/com/leadera/leadera/controller/OperacionController.java \
  leadera/src/test/java/com/leadera/leadera/integration/CrossTenantWriteTest.java
git commit -m "security: reemplazar entity binding por DTOs en OperacionController (cross-tenant fix)"
```

---

## Task 9: Supabase Storage RLS — cerrar bucket público (acción manual)

**Files:**
- No hay código que cambiar. El backend ya usa `service_role` key en `SupabaseStorageService.java`.
- Acción requerida: configuración en Supabase dashboard.

**Contexto:** El bucket `fotos-propiedades` en Supabase puede tener políticas que permiten INSERT al rol `anon` (sin autenticación). Cualquier persona con la URL del bucket puede subir archivos directamente sin pasar por el backend. El backend ya usa la `service_role` key para subir, pero si el bucket no tiene RLS habilitado, los uploads directos siguen siendo posibles.

- [ ] **Step 1: Habilitar RLS en el bucket (Supabase dashboard)**

1. Ir a **Supabase → Storage → fotos-propiedades → Policies**
2. Si `RLS` está desactivado, activarlo.
3. Eliminar cualquier política que diga `INSERT` para el rol `anon` o `public`.
4. La política correcta para este bucket es: solo `SELECT` público (para servir imágenes), y `INSERT`/`DELETE` solo con `service_role` (que el backend usa).

Política de ejemplo a crear (solo lectura pública):
```sql
-- Permitir lectura pública de objetos del bucket
CREATE POLICY "Lectura pública de fotos"
ON storage.objects FOR SELECT
USING (bucket_id = 'fotos-propiedades');
```

No crear ninguna política de INSERT para `anon`.

- [ ] **Step 2: Verificar que el upload desde el backend sigue funcionando**

Levantar el backend en local (o usar el de Render) y subir una foto desde el frontend. Verificar que la imagen se guarda y se muestra en la propiedad.

- [ ] **Step 3: Verificar que el upload directo al bucket sin token es rechazado**

Intentar un upload directo al bucket usando solo la `anon key` (sin pasar por el backend):

```bash
curl -X POST \
  "https://<tu-proyecto>.supabase.co/storage/v1/object/fotos-propiedades/test.jpg" \
  -H "Authorization: Bearer <anon-key>" \
  -H "Content-Type: image/jpeg" \
  --data-binary @/dev/null
```

La respuesta debe ser `403 Forbidden` o `401 Unauthorized`. Si es `200`, las políticas no están configuradas correctamente.

- [ ] **Step 4: Verificar env vars en Render**

Confirmar que `SUPABASE_SERVICE_ROLE_KEY` está seteada en Render (sin esta variable el backend no puede subir fotos en producción).

---

## Resumen de cambios por fase

| Task | Archivos | Tipo |
|------|----------|------|
| 1 | `environment.ts` | Código (trivial) |
| 2 | `SecurityConfig.java` + Render dashboard | Código + acción manual |
| 3 | `JwtAuthenticationFilter.java` | Verificación (ya resuelto) |
| 4 | `dto/CrearOperacionRequest.java` | Nuevo archivo |
| 5 | `dto/OperacionDTO.java` | Nuevo archivo |
| 6 | `service/OperacionService.java` | Modificación |
| 7 | `controller/OperacionController.java` | Modificación |
| 8 | `integration/CrossTenantWriteTest.java` | Nuevos tests |
| 9 | Supabase dashboard | Acción manual |

**Gate de salida:** `./mvnw test` en verde + los dos tests nuevos pasando + bucket Supabase sin INSERT anónimo.
