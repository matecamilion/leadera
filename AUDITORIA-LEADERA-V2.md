# 🔍 Reporte de Análisis de Código — LeadEra

**Fecha:** 2026-06-10
**Stack:** Spring Boot 3.3 / Java 17 / Angular 20 / Supabase PostgreSQL
**Versión analizada:** post multi-tenant (commit 111a685, + eca0997 en working tree)
**Analizado por:** Claude Code

Auditoría delta respecto a la del 2026-05-24 (commit 35cacf0). Los 3 críticos de esa auditoría (IDOR dashboard, IDOR interacciones, secrets con fallback hardcodeado) están **verificados como resueltos**: `LeadController.resolverAgenteId()` deriva el id del token (LeadController.java:49-53), `obtenerHistorialInteracciones` valida ownership (LeadService.java:144), y `application-dev.properties` ya no tiene fallbacks de secretos (líneas 3-4 lo documentan explícitamente).

---

## 📊 Score General

| Área | Score | Estado |
|------|-------|--------|
| 🔐 Seguridad (×2) | 6/10 | 🟡 |
| ⚡ Performance (×2) | 6/10 | 🟡 |
| 📈 Escalabilidad | 6/10 | 🟡 |
| 🧹 Calidad de Código | 6.5/10 | 🟡 |
| 🔧 Mantenibilidad | 6/10 | 🟡 |
| **🏆 Score Global ponderado** | **6.1/10** | 🟡 |

> 🔴 < 5 | 🟡 5-7 | 🟢 > 7

**Lectura honesta del 6.1:** es el mismo número que en mayo, pero la composición cambió. Lo que era rojo conocido (IDOR de lectura, secrets) se cerró, y el multi-tenant se implementó con un nivel de cuidado alto en las **lecturas** (aislamiento testeado con 13 tests de integración). Pero esta auditoría encontró **un crítico nuevo de escritura cross-tenant** que las lecturas blindadas no cubren, y la deuda de performance (paginación en memoria, N+1 en stats) sigue intacta y ahora se multiplica por agente en `/inmobiliaria/stats`.

---

## 🔄 FODA Técnico

### ✅ Fortalezas
- **Aislamiento de lectura multi-tenant sólido y testeado**: `MultiTenantIntegrationTest` cubre 6 escenarios de aislamiento real (leads ajenos, matching cross-tenant, stats, desactivación de agentes ajenos) con MockMvc + H2. Es el área mejor testeada del producto.
- **El matching compartido no filtra PII**: `CompradorPotencialDTO` omite teléfono/email de leads ajenos a propósito (PropiedadService.java:207-208) y hay un test que verifica byte a byte que no viajan (MultiTenantIntegrationTest.java:246-251).
- **Cadena de seguridad bien razonada**: orden de matchers documentado (SecurityConfig.java:80-82), `@PreAuthorize("hasRole('DUENO')")` a nivel clase en InmobiliariaController, agentes desactivados bloqueados también con token vigente (JwtAuthenticationFilter.java:59-61).
- **Migración SQL de prod prolija**: transaccional, con backfill por loop, verificaciones post-commit y advertencia de backup (migrate_inmobiliaria_postgres.sql).
- **Queries batch donde duele**: `countLeadsActivosPorAgente`, `countByLeadIdsGroupedByTipo`, `findPrimerasInteraccionesPorAgente` evitan N+1 en listados y dashboard.

### ⚠️ Debilidades
- Escritura cross-tenant posible vía entity binding (ver Crítico abajo).
- Bucket de Supabase Storage escribible con la anon key pública.
- 4 endpoints siguen devolviendo entidades JPA; 2 las aceptan como request body.
- Sin Flyway: 4 scripts SQL sueltos, uno de ellos inconsistente con el schema actual.
- Stats de agente con N+1 que el método batch existente podría eliminar hoy mismo.

### 🚀 Oportunidades
- El modelo Inmobiliaria/RolAgente quedó simple y extensible: agregar roles (SUPERVISOR) o límites por plan es barato.
- La estructura de tests de integración ya armada hace que blindar cada endpoint nuevo cueste ~20 líneas.
- `EquipoStatsDTO` reutilizando el cache por agente es una buena base para un dashboard de dueño vendible.

### 🔴 Amenazas
- **El aislamiento depende de disciplina, no de arquitectura**: cada query nueva tiene que acordarse de filtrar por agente/inmobiliaria. Con un solo dev, una query olvidada = fuga de datos entre inmobiliarias con clientes reales.
- Datos reales de un cliente en producción + Ley 25.326: una fuga cross-tenant hoy no es un bug, es un problema legal y reputacional en un mercado chico como Mar del Plata.
- Render free/starter + pool de 3 conexiones + cache y rate-limit en memoria: cualquier segundo nodo o restart degrada silenciosamente.

---

## 🔐 Seguridad — 6/10

### 🔴 Crítico — Escritura cross-tenant vía entity binding en `POST /propiedades/lead/{leadId}`

**Archivo:** PropiedadController.java:32-37 → PropiedadService.java:53-64, Propiedad.java:26-28

El endpoint acepta la **entidad** `Propiedad` como `@RequestBody`. El service hace:

```java
propiedad.setLead(lead);
propiedad.setFechaPublicacion(LocalDateTime.now(zonaHoraria));
return PropiedadMapper.toDTO(propiedadRepository.save(propiedad));
```

`Propiedad.id` es `long` con `@GeneratedValue(IDENTITY)`. Si un atacante autenticado (cualquier agente de cualquier inmobiliaria) envía `{"id": 47, "direccion": "pwned", ...}` apuntando a un `leadId` propio, `save()` detecta `id != 0` → **merge → UPDATE sobre la fila 47**, que puede ser la propiedad de otra inmobiliaria. Resultado: roba la propiedad (le reasigna `lead_id` a su propio lead) y pisa todos sus datos. Los IDs son secuenciales: enumerarlos es trivial.

**Impacto:** un competidor con una cuenta de $0 puede corromper o apropiarse del inventario de propiedades de tu cliente real. El aislamiento de lectura (que está muy bien hecho) no protege contra esto.

**Mismo patrón en** `POST /leads/{leadId}/operaciones/{operacionId}/eventos` (OperacionController.java:99-109): `EventoOperacion` entra crudo y `eventoOperacionRepository.save(evento)` (OperacionService.java:209) con un `id` ajeno en el body hace UPDATE sobre el evento de otra operación. El de operaciones (`crearOperacion`) se salva porque construye `new Operacion()` y copia campo por campo (OperacionService.java:76-84) — ese es exactamente el patrón a replicar.

**Recomendación:** DTO de request (`CrearPropiedadRequest`, `CrearEventoRequest`) sin campo `id`, construcción explícita de la entidad. Mitigación de una línea mientras tanto: `propiedad.setId(0)` / `evento.setId(null)` antes del save.

```java
// Actual (PropiedadService.java:53)
public PropiedadDTO agregarPropiedad(Long leadId, Propiedad propiedad, String emailAgente) { ... }

// Corregido
public PropiedadDTO agregarPropiedad(Long leadId, CrearPropiedadRequest request, String emailAgente) {
    // ... validación de ownership igual que ahora ...
    Propiedad propiedad = new Propiedad();
    propiedad.setDireccion(request.getDireccion());
    // ... resto de campos, sin id ...
}
```

### 🟠 Alto — Bucket de Supabase Storage escribible con la anon key pública

**Archivo:** foto-propiedad-service.ts:20-45, environment.prod.ts:4-5

La subida de fotos va directo del browser a Supabase Storage autenticada **solo con la anon key**, que viaja en el bundle JS de Vercel (es pública por diseño). Para que esto funcione, el bucket `fotos-propiedades` tiene una policy de INSERT para el rol `anon`. Consecuencia: **cualquier persona en internet** puede subir archivos arbitrarios e ilimitados al bucket (sin pasar por la app, con `curl`), y el bucket es de lectura pública (`/object/public/`). Riesgos: hosting de malware/contenido ilegal bajo tu dominio de Supabase, agotamiento de storage (costo), y borrado: el backend borra la fila (`FotoPropiedadService.eliminarFoto`) pero **nunca borra el objeto del storage** → huérfanos acumulándose.

**Recomendación:** la subida debe pasar por el backend (endpoint que valide tipo/tamaño y suba con la `service_role` key desde Render), o al menos: policy de INSERT restringida + límite de tamaño/MIME en el bucket + job de limpieza. La anon key en el repo no es un secreto filtrado per se, pero la policy permisiva sí es la vulnerabilidad.

### 🟠 Alto — Token expirado/malformado produce 500, y sin token produce 403: el contrato de auth está roto

**Archivos:** JwtAuthenticationFilter.java:53, SecurityConfig.java:74-97, error-interceptor.ts:26-31

Dos problemas que se combinan:

1. `jwtService.extractUsername(jwt)` (JwtAuthenticationFilter.java:53) no tiene try/catch. Un token expirado lanza `ExpiredJwtException` **dentro del filtro**, donde el `GlobalExceptionHandler` no llega → 500 al cliente. Como el JWT dura 24h y el guard de Angular solo chequea `exp` al navegar (auth-guard.ts:19), un usuario con la pestaña abierta de un día para el otro ve "Error interno del servidor" en vez de ser redirigido al login.
2. `SecurityConfig` no define `authenticationEntryPoint`, así que Spring usa `Http403ForbiddenEntryPoint`: toda request sin token a un endpoint protegido devuelve **403, no 401**. El `error-interceptor` solo desloguea en 401 (línea 26); el 403 lo muestra como "No tenés permiso" y deja al usuario clavado.

**Relación con el bug conocido (`POST /auth/cambiar-password` → 403 con `debeCambiarPassword=true`):** registrado como hallazgo, no corregido. La evidencia apunta a que el backend está sano: el test de integración que reproduce exactamente ese flujo está en verde (MultiTenantIntegrationTest.java:354-371), la cadena de seguridad es correcta (SecurityConfig.java:82) y el interceptor de Angular fue corregido en eca0997 (auth-interceptor.ts:6 ya solo excluye login/register). Hipótesis más probable: **el frontend deployado en Vercel es anterior a eca0997** y sigue mandando la request sin header → Spring responde 403 por el punto 2 de arriba (en vez del 401 que hubiera hecho obvio el diagnóstico). Verificar qué commit está deployado antes de buscar más.

**Recomendación:** try/catch en el filtro (token inválido → continuar sin autenticar), y `.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))` para que "sin credenciales" sea 401 y "sin permisos" sea 403. Eso solo ya hace que el error-interceptor redirija bien.

### 🟡 Medio — JWT en localStorage (preexistente, sin cambios)

**Archivo:** auth-service.ts:35, auth-interceptor.ts:13

Ya señalado en la auditoría anterior; sigue igual. Cualquier XSS exfiltra el token de 24h. Angular sanitiza templates por defecto y no vi `innerHTML`/`bypassSecurityTrust` en las páginas revisadas, así que el riesgo es latente, no activo. La migración a httpOnly cookies implica CSRF tokens y cambios de CORS; razonable dejarlo para antes de escalar, no para esta semana. Mitigación barata intermedia: bajar la expiración del token (hoy además está hardcodeada, ver Bajo).

### 🟡 Medio — CORS con `allowedOriginPatterns` + comodín de Vercel + `allowCredentials(true)`

**Archivo:** SecurityConfig.java:58-65

El comentario dice que el patrón soporta `https://leadera-*.vercel.app` para previews. Si `CORS_ALLOWED_ORIGINS` en Render efectivamente contiene ese patrón: **cualquiera puede crear un proyecto Vercel llamado `leadera-loquesea`** y su dominio matchea el patrón → origen hostil con CORS aprobado y credenciales. Con el token en localStorage el impacto real es bajo (CORS no regala el localStorage de otro origen), pero si migrás a cookies httpOnly esto se convierte en crítico instantáneo. Verificar el valor real de la env var; idealmente listar orígenes exactos en prod.

### 🟡 Medio — Email del usuario en logs WARN

**Archivo:** JwtAuthenticationFilter.java:71 (`log.warn("Token inválido para: {}", userEmail)`)

WARN está activo en prod (Render persiste logs). El email es dato personal bajo la Ley 25.326 — el propio CLAUDE.md del proyecto pide logs sin PII. Los `log.debug` de las líneas 54 y 62 no salen en prod, pero el warn sí. Loguear un hash o los primeros caracteres.

### 🔵 Bajo — Rate limit evadible por `X-Forwarded-For` y mapa sin límite de tamaño

**Archivo:** RateLimitFilter.java:71-78

Se toma el **primer** hop del header, que es el que controla el cliente (los proxies appendean). Un atacante rota IPs ficticias en el header → bucket nuevo por request → bypass del límite de login y crecimiento sin tope del `ConcurrentHashMap` (memoria). En Render conviene tomar el **último** hop (el que agrega el proxy de Render) o `request.getRemoteAddr()`, y acotar el mapa (Caffeine con `maximumSize` + `expireAfterAccess`).

### 🔵 Bajo — Expiración del JWT hardcodeada, config muerta

**Archivo:** JwtService.java:46

`app.jwt.expiration-ms` existe en los 3 properties y en Render, pero `generarToken` hardcodea `1000 * 60 * 60 * 24`. La env var no hace nada. Inyectar `@Value("${app.jwt.expiration-ms}")`.

**Sin problemas detectados en:** BCrypt (sin fallbacks), validación de ownership en todos los GET/PUT/DELETE de leads, operaciones, propiedades y fotos (patrón consistente), `cambiarActivo` con 404 para no revelar existencia cross-tenant (InmobiliariaService.java:78-82), registro público atómico con `@Transactional`.

---

## ⚡ Performance — 6/10

### 🟠 Alto — Paginación en memoria por `JOIN FETCH` de colección

**Archivo:** LeadRepository.java:40-48 (`findByAgenteEmailConInteraccionesPaginado`) y 103-112 (`findByInmobiliariaConInteraccionesPaginado`)

`Page<Lead>` + `LEFT JOIN FETCH l.interacciones` dispara el clásico `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`. Hibernate trae **todas** las filas del agente/inmobiliaria (leads × interacciones) y pagina en RAM. Hoy con un usuario no se nota; con una inmobiliaria de 10 agentes × 500 leads × 20 interacciones, el listado del dueño (`/inmobiliaria/leads`) materializa ~100.000 filas por página pedida, en un dyno de Render con pool de 3 conexiones.

**Recomendación:** el DTO solo necesita `interacciones.size()` y el detalle de la última (LeadService.java:408-410). Reemplazar el fetch por una query de conteos agrupados (como ya hacés con `countByLeadIdsGroupedByTipo`) + una query de últimas interacciones por lead de la página. Dos queries fijas, paginación real en SQL.

### 🟠 Alto — N+1 en `obtenerEstadisticasAgente`, amplificado por `/inmobiliaria/stats`

**Archivo:** LeadService.java:274-283

```java
List<Lead> leads = leadRepository.findLeadsConFechaEntrada(agenteId);   // todos los leads
... lead -> interaccionRepository.findPrimeraInteraccion(lead.getId())  // 1 query POR lead
```

Lo irónico: el método batch que lo arregla **ya existe** (`InteraccionRepository.findPrimerasInteraccionesPorAgente`, línea 27-29) y `DashboardService.cargarPrimerasInteracciones` (DashboardService.java:227-233) ya lo usa. Acá quedó la versión vieja. `InmobiliariaService.obtenerStats` (línea 113-114) llama esto **en loop por cada agente activo**: primer hit sin cache con 10 agentes × 200 leads = ~2.000 queries para pintar la pestaña Stats del dueño. El cache de 5 min lo disimula después del primer hit, pero el primer hit puede tumbar el pool.

**Recomendación:** copiar el patrón de DashboardService (3 líneas). Bonus: el conteo de leads por estado son 4 queries (líneas 258-264) que pueden ser 1 con `GROUP BY estado`.

### 🟡 Medio — `findLeadsConFechaEntrada` cargado dos veces por dashboard

**Archivo:** DashboardService.java:170-171 → 211

`tiempoRespuestaPromedioDias` se llama para el período actual y el anterior, y **cada llamada** ejecuta `leadRepository.findLeadsConFechaEntrada(agenteId)` trayendo todas las entidades Lead completas. Cargar la lista una vez y pasarla, o mejor: una query que devuelva solo `(id, fechaEntrada)`.

### 🟡 Medio — Serialización de entidades dispara lazy loading vía OSIV

**Archivos:** LeadController.java:80,86,96,101,107 / OperacionController.java:25-49

Los endpoints que devuelven `Lead` crudo serializan `interacciones` y `propiedades` (con `fotos` adentro) completas gracias a Open-Session-In-View (activo por defecto, sin `spring.jpa.open-in-view=false` en ningún properties). Cada lead del response = 2+ queries extra en serialización + payloads enormes que el frontend ni usa. Se resuelve junto con el punto de DTOs de Calidad.

### 🔵 Bajo — Matching en Java sobre todos los candidatos del tenant

**Archivo:** PropiedadService.java:159-162, OperacionRepository.java:112-124

`findCandidatosParaMatching` trae todas las operaciones COMPRA abiertas de la inmobiliaria y filtra en Java. Con 50 inmobiliarias de 10 agentes esto es ~cientos de filas por click en "compradores potenciales" — aceptable. El comentario del repo explica bien el trade-off (evitar mapeo de enums en JPQL). No urgente, pero documentar el límite.

**Bien resuelto:** índices compuestos por agente (leadera_indexes.sql), Hikari acotado a Supabase (prod:18-21), Caffeine con TTL y maxSize (CacheConfig.java:20-23), evicts de cache acotados por agente con keys consistentes (LeadService.java:79, 157, 212, 250-253).

---

## 📈 Escalabilidad — 6/10

### ¿Soporta 50 inmobiliarias × 10 agentes (500 usuarios)?

**El modelo de datos: sí.** Tenant por FK (`agente.inmobiliaria_id`) con derivación lead→agente→inmobiliaria es el approach correcto para esta escala; pool compartido con discriminador es exactamente lo que usan SaaS mucho más grandes. El índice `idx_agente_inmobiliaria` está creado.

**La implementación: con 3 reparaciones, sí.** Los cuellos concretos en orden de aparición:

1. **Pool de 3 conexiones + paginación en memoria + stats N+1** (ver Performance). Con 500 usuarios, 3 conexiones se saturan con 2-3 dueños abriendo stats a la vez. El pool de 3 es por el límite de Supabase en plan free — antes de vender a 5+ inmobiliarias hay que pasar a Supavisor/pgBouncer en modo transaction y subir el pool.
2. **Estado en memoria por instancia**: rate limit (RateLimitFilter.java:37) y cache (Caffeine) viven en el proceso. En Render con 1 instancia funciona; con autoscaling o un segundo nodo, el rate limit se duplica efectivamente y el cache se fragmenta (stats inconsistentes entre requests). No bloquea 50 inmobiliarias en 1 instancia, pero es el techo de la arquitectura actual. Documentarlo como restricción consciente.
3. **El aislamiento es por convención, no estructural** — el riesgo de escala más importante. Hoy hay ~40 queries en repositorios y **todas** filtran por agente o inmobiliaria... porque alguien se acordó de hacerlo en cada una. `LeadService.obtenerLeads()` → `findAll()` sin filtro (LeadService.java:101-103) existe y está a un autocomplete de ser usada en un controller (hoy es código muerto, junto con `LeadRepository.existsByEmail`). A medida que el equipo o el código crezcan, la probabilidad de una query sin filtro tiende a 1. Opciones: Hibernate `@Filter` con `inmobiliaria_id` activado por interceptor, o un test de arquitectura (ArchUnit) que falle si un método de repository no incluye agente/inmobiliaria en la firma. Para PostgreSQL gestionado, RLS de Supabase es la versión más robusta.
4. **Sin límites por plan**: un dueño puede crear agentes ilimitados (`InmobiliariaService.crearAgente` no tiene tope). Cuando haya pricing por asiento, esto es revenue leak directo.

### 🔵 Bajo — `DashboardService` usa `LocalDate.now()` sin zona

**Archivo:** DashboardService.java:55

Todo el resto del código usa `LocalDateTime.now(zonaHoraria)` (el bean de AppConfig). Acá se usa la zona del server (UTC en Render): entre las 21:00 y las 00:00 hora argentina, el dashboard cuenta "hoy" como mañana. Inconsistencia menor pero visible para el usuario real.

---

## 🧹 Calidad de Código — 6.5/10

### 🟠 Alto — 4 endpoints siguen devolviendo entidades JPA, 2 las aceptan de entrada

Pendiente de la auditoría anterior, sin avance:

| Endpoint | Archivo | Problema |
|---|---|---|
| `GET /leads/estado/{estado}`, `/sin-contactar`, `/inactivos`, `/prioritarios` | LeadController.java:80, 86, 96, 101 | Devuelven `List<Lead>` con grafo completo |
| `PUT /leads/{id}/estado` | LeadController.java:107 | Devuelve `Lead` (nota: `PATCH /{id}/estado` al lado ya devuelve DTO — inconsistencia en el mismo archivo) |
| `POST/GET /leads/{leadId}/operaciones*` | OperacionController.java:25, 40, 53, 67, 83 | `Operacion` como request y response body, sin `@Valid` |
| `POST /propiedades/lead/{leadId}` | PropiedadController.java:34 | `Propiedad` como request body (es el vector del Crítico de seguridad) |
| `POST .../interacciones` (response) | InteraccionController.java:22 | Devuelve `Interaccion` entidad (el request sí es DTO) |

El contraste duele porque el código nuevo lo hace bien: todo `/inmobiliaria/**` es DTO puro con `@Valid`, igual que el pipeline (`OperacionPipelineDTO`). Es deuda vieja, no regresión, pero el crítico de seguridad demuestra que no es solo cosmético.

### 🟡 Medio — Lombok `@Data` en entidades JPA

**Archivos:** Agente.java:18, Lead.java:16, Inmobiliaria.java:10, Propiedad.java:21, EventoOperacion.java:13

`@Data` genera `equals`/`hashCode` que incluyen colecciones y relaciones lazy: riesgo de `LazyInitializationException` en usos con Sets y de recursión Lead↔Interaccion. `Agente` además implementa `UserDetails` — un `toString()` accidental en un log serializa medio grafo (hay `@ToString.Exclude` solo en `leads`, Agente.java:61). Patrón estándar: `@Getter @Setter` + `equals/hashCode` solo por id (como ya hace `Operacion`, que usa `@Getter @Setter` — el criterio existe en el propio repo).

### 🔵 Bajo — Código muerto y prolijidad

- `LeadService.obtenerLeads()` (línea 101) y `LeadRepository.existsByEmail` (línea 22): sin usos. Borrar (el primero es además un footgun multi-tenant).
- `import org.apache.coyote.Response` y `import lombok.Getter` sin uso en LeadController.java:22-23.
- `pom.xml:14-28`: bloques `<name/>`, `<licenses><license/></licenses>` vacíos.
- Frontend: rutas todas eager (app.routes.ts importa los 24 componentes); con standalone components, `loadComponent` es un cambio mecánico que reduce el bundle inicial.

**Bien:** GlobalExceptionHandler completo (enums inválidos, type mismatch, data integrity, mensajes sin leak de credenciales), validadores espejo backend/frontend en el form de equipo (equipo.ts:51-56 replica `CrearAgenteEquipoRequest`), comentarios que explican el "por qué" (la nota sobre el orden de filtros en SecurityConfig.java:90-93 es ejemplar), generación de password temporal con `crypto.getRandomValues` (equipo.ts:95-105).

---

## 🔧 Mantenibilidad — 6/10

### 🟠 Alto — Migraciones manuales sin versionado, con un script ya inconsistente

**Archivos:** `resources/db/*.sql`

Cuatro scripts sueltos que dependen de que un humano los corra en el orden correcto contra la base correcta. El riesgo no es teórico, ya hay síntomas:

- `migrate_estado_operacion_2026_05_26.sql:20-21` referencia la tabla **`operaciones`** y la columna **`estado`** tipo `ENUM` de MySQL. La entidad actual mapea `@Table(name="operacion")` (Operacion.java:18) y la columna sería `estado_operacion`. Ese script ya no corresponde al schema real — quien lo corra "para ponerse al día" rompe o no hace nada, sin saber cuál de las dos.
- `leadera_indexes.sql` usa `CREATE INDEX IF NOT EXISTS`, que **MySQL no soporta** (sí Postgres/H2): el script "para MySQL en dev" falla en MySQL.
- Hay scripts gemelos MySQL/Postgres (migrate_inmobiliaria_*) que ya divergen en semántica transaccional, como el propio header documenta.

Con `ddl-auto=validate` en prod, un deploy con el script sin correr = backend caído (al menos falla rápido). **Recomendación:** Flyway con `V1__baseline.sql` del schema actual de Supabase. Es una dependencia nueva (según tus reglas, te la listo: `org.flywaydb:flyway-core` + `flyway-database-postgresql`, justificación: elimina la clase entera de errores "script no corrido / corrido dos veces / en orden incorrecto" antes de tener N clientes con N bases en N estados). Encaja además con tu migración planeada MySQL→Postgres.

### 🟡 Medio — Cobertura de tests: excelente donde existe, nula en flujos de negocio críticos

35 `@Test` (15 integración multi-tenant, 12 LeadService, 7 InteraccionService, 1 contextLoads). Los de integración usan H2 correctamente: perfil `test` aislado (application-test.properties con H2, `create-drop`, secret de test ≥32 bytes), limpieza por FK-order en `setUp`, y clear del cache entre tests — bien hecho. Sin cubrir:

- `OperacionService`: la máquina de estados de transiciones (TRANSICIONES_VALIDAS) y la sincronización propiedad↔operación — la lógica de negocio más densa del backend, 0 tests.
- `AuthService.cambiarPassword` a nivel unitario (passwords iguales, password actual incorrecta).
- `DashboardService` completo.
- Frontend: specs autogenerados sin lógica (sin cambios desde la auditoría anterior).

**Nota H2 vs Postgres:** los tests de integración validan lógica, no dialecto. `MONTH()/YEAR()` en JPQL (LeadRepository.java:82-83) funcionan en ambos, pero H2 no va a detectar problemas de tipos/collation de Postgres. Aceptable hoy; cuando entre Flyway, considerar Testcontainers para 2-3 tests de humo.

### 🟡 Medio — `environment.ts` de desarrollo apunta a producción

**Archivo:** environment.ts:3

`apiUrl: 'https://leadera-42po.onrender.com'` en el environment de **dev**. Todo `ng serve` local pega contra el backend de prod con datos del cliente real. Un dev probando "crear lead" o "eliminar lead" toca datos reales. Debería ser `http://localhost:8080`. (El environment.prod.ts está bien.)

### 🔵 Bajo — Sin OpenAPI/Swagger, sin CI

~35 endpoints documentados solo en el código. `springdoc-openapi` es una dependencia (te aviso por la regla 4) que se paga sola en cuanto integres n8n o un tercero. No hay workflow de CI que corra los 35 tests en push — con OneDrive como "backup" del working tree, un `git push` + GitHub Actions de 10 líneas es la red de seguridad más barata disponible.

---

## 🎯 Plan de Acción Priorizado

### 🔴 Inmediato (antes de conseguir el próximo cliente)

1. **Cerrar la escritura cross-tenant**: DTOs de request en `POST /propiedades/lead/{leadId}` y `POST .../eventos` (o mínimo anular el `id` entrante). Agregar el test de integración "un agente de B no puede pisar la propiedad de A por id" al suite multi-tenant. *Esfuerzo: medio día.*
2. **Restringir el bucket de Supabase Storage**: quitar INSERT del rol anon, límite de tamaño/MIME. Decidir si la subida pasa por el backend. *Esfuerzo: 1-2 días según opción.*
3. **Arreglar el contrato 401/403 + try/catch en el JWT filter**, y verificar que el deploy de Vercel incluya eca0997 — eso debería cerrar el bug conocido de `cambiar-password`. *Esfuerzo: medio día.*
4. **`environment.ts` de dev → localhost.** *Esfuerzo: 5 minutos.*

### 🟡 Corto plazo (próximas 2-4 semanas)

5. Eliminar el N+1 de `obtenerEstadisticasAgente` usando el método batch que ya existe, y matar la paginación en memoria de los dos listados paginados.
6. Terminar la migración a DTOs de los 4 endpoints que devuelven entidades (cerrar de una vez el pendiente de mayo) + `spring.jpa.open-in-view=false`.
7. Flyway con baseline del schema actual (resuelve también el script inconsistente de estado_operacion).
8. Tests de `OperacionService` (máquina de estados) y `AuthService.cambiarPassword`. CI mínimo en GitHub Actions.
9. PII fuera de los logs WARN; expiración JWT desde config.

### 🔵 Largo plazo (antes de escalar)

10. Aislamiento estructural de tenant: Hibernate `@Filter` o RLS en Supabase + test ArchUnit que impida queries sin filtro.
11. httpOnly cookies + refresh tokens (junto con el endurecimiento de CORS a orígenes exactos).
12. Pool de conexiones vía Supavisor + revisar plan de Render; rate limit y cache externalizables si hay segunda instancia.
13. Límites por plan (asientos por inmobiliaria) cuando se defina pricing.
14. OpenAPI para integraciones (n8n, portales).

---

## 💡 Recomendación Estratégica

**¿Está LeadEra en condiciones de venderse como SaaS en Mar del Plata hoy? Casi, pero no esta semana.** La distancia al "sí" es la más corta que tuvo el producto: el multi-tenant —la única feature sin la cual no había nada que vender— existe, funciona y está mejor testeado que el resto del sistema. Lo que bloquea no es falta de producto, son los puntos 1-3 del plan inmediato: una vulnerabilidad de escritura cross-tenant explotable con una cuenta gratuita, un bucket de storage abierto a internet, y un flujo de onboarding de agentes (password temporal) que hoy termina en un 403. Los tres juntos son ~3 días de trabajo. Venderle a una segunda inmobiliaria antes de cerrarlos sería poner en riesgo los datos de la primera.

**Los 3 movimientos del próximo mes, en orden:**

1. **Semana 1 — Cerrar los rojos y el bug de onboarding.** El flujo dueño-crea-agente → agente-entra → cambia-password es literalmente el momento de la venta (es lo primero que el dueño hace en la demo). Tiene que ser impecable.
2. **Semanas 2-3 — Performance del lado del dueño + Flyway.** El dashboard de equipo (`/inmobiliaria/stats`, `/inmobiliaria/leads`) es tu argumento de venta frente al dueño de la inmobiliaria, y es exactamente donde viven la paginación en memoria y el N+1. Que la pantalla que vende sea la más rápida, no la más lenta. Flyway entra acá porque cada cliente nuevo = una presión más sobre el proceso manual de migraciones.
3. **Semana 4 — Salir a vender con un onboarding ensayado**, no a seguir codeando features. El producto ya tiene pipeline kanban, matching compartido, gestión del día, dashboard, equipo y fotos: es más completo que lo que usa el 90% de las inmobiliarias chicas (planillas y WhatsApp). El cuello de botella ahora es comercial, y cada cliente real te va a priorizar el backlog mejor que cualquier auditoría — incluida esta.

El delta desde mayo es real: de "3 críticos conocidos y sin multi-tenant" a "1 crítico nuevo encontrado y multi-tenant testeado". Si el ciclo se repite (cerrar rojos → vender → auditar), la próxima auditoría debería ser la primera en verde.
