# LeadEra — Auditoría Técnica y de Producto

**Fecha:** 2026-05-24
**Autor:** Auditoría técnica externa
**Alcance:** Backend (Spring Boot 3.3 / Java 17 / JPA / MySQL-PostgreSQL) + Frontend (Angular 20 / Signals) + Automatización (n8n)
**Versión analizada:** `main` @ commit `35cacf0` (feat(matching): compradores potenciales por propiedad)

---

## Índice

1. Resumen ejecutivo
2. Exploración del repositorio
3. Análisis por área
   - A. Experiencia de usuario (UX/UI)
   - B. Funcionalidades y lógica de negocio
   - C. Arquitectura y calidad de código
   - D. Seguridad
   - E. Base de datos y performance
   - F. Potencial de automatización con n8n
4. Oportunidades de producto
5. FODA técnico-comercial
6. Scoring por área
7. Plan de acción priorizado
8. Conclusión ejecutiva

---

## 1. Resumen ejecutivo

LeadEra es un MVP **funcional y deployado** que demuestra dominio de los fundamentos: Spring Security + JWT, JPA con queries optimizadas para el flujo crítico (`/leads/hoy`), separación con DTOs en lecturas calientes, frontend Angular 20 con Signals, rate limiting en login, cache con Caffeine, manejo global de excepciones, y aislamiento multi-tenant por `agente.email`.

**Sin embargo**, el producto tiene **3 problemas críticos de seguridad** que lo hacen NO apto para vender hoy:

1. **IDOR en `/leads/agente/{id}/...`** — cualquier agente autenticado puede ver el dashboard, stats y actividad reciente de otros agentes sustituyendo el `id` en la URL.
2. **`GET /leads/{id}/interacciones` sin validación de propietario** (el código tiene incluso un `// Aquí podrías agregar seguridad también` admitiendo el bug).
3. **Secrets de fallback hardcodeados** en `application-dev.properties`: `DB_PASSWORD=Matecamilion2005` y `JWT_SECRET=cambiar-este-secreto-...` checkeados al repo. Si en Render falla la inyección de env vars, el sistema arranca con esos defaults.

A esto se suman: tokens en `localStorage` (vulnerable a XSS), exposición de entidades JPA directamente en respuestas HTTP, N+1 en estadísticas y resumen de leads, y ausencia de notificaciones/recordatorios (que es el corazón de un CRM inmobiliario).

**Veredicto:** producto a **2-4 semanas de ser comercializable** si se atacan los críticos primero. La base arquitectónica es sana; los problemas son de ejecución concreta, no de diseño.

**Score global ponderado: 6.1 / 10 (Regular alto, con techo claro hacia "Bueno" si se cierran los rojos).**

---

## 2. Exploración del repositorio

### Estructura

```
crm-inmobiliario/
├── leadera/                # Backend Spring Boot
│   ├── pom.xml             # Spring Boot 3.3.0, Java 17, JJWT 0.11.5, Bucket4j, Caffeine, MySQL + Postgres drivers
│   ├── src/main/java/com/leadera/leadera/
│   │   ├── config/         # SecurityConfig, JwtAuthenticationFilter, RateLimitFilter, CacheConfig, WebConfig, AppConfig
│   │   ├── controller/     # 7 controllers REST
│   │   ├── dto/            # ~17 DTOs (mezcla DTOs y un par de entidades expuestas)
│   │   ├── entity/         # Agente, Lead, Interaccion, Propiedad, Operacion, Busqueda, EventoOperacion, FotoPropiedad
│   │   ├── enums/          # EstadoLead, EstadoOperacion, EstadoPropiedad, TipoEvento, TipoInteraccion, TipoOperacion, TipoVivienda
│   │   ├── exception/      # ApiError + 4 custom + GlobalExceptionHandler
│   │   ├── mapper/         # LeadMapper (sólo uno)
│   │   ├── repository/     # 7 repos JPA
│   │   └── service/        # 7 services + CustomUserDetailService
│   ├── src/main/resources/
│   │   ├── application{,-dev,-prod}.properties
│   │   └── db/leadera_indexes.sql  # índices manuales, sin Flyway
│   └── src/test/java/      # 3 archivos de test (Mockito, parcial)
├── leadera-front/          # Angular 20 standalone + Signals
│   ├── package.json        # @angular/cdk, xlsx-js-style, sin librerías de UI
│   └── src/app/
│       ├── core/{guards,models,services}/
│       ├── interceptors/   # auth + error + loading
│       ├── pages/          # 19 páginas (home, listar-leads, kanban-board, perfil, etc.)
│       ├── shared/         # sidebar, header, footer, lead-card, lead-section
│       ├── components/fotos-propiedad/
│       └── pipes/
├── CLAUDE.md, agents.md, memory.md, README.md
```

### Stack y deps confirmados

| Capa | Tecnología |
|---|---|
| Backend runtime | Java 17, Spring Boot 3.3.0 |
| Persistencia | JPA/Hibernate, MySQL (dev), PostgreSQL (prod), Hikari |
| Seguridad | Spring Security 6, JJWT 0.11.5, BCrypt, Bucket4j 8.10.1 |
| Cache | Caffeine + Spring `@Cacheable` |
| Frontend | Angular 20, RxJS 7.8, standalone components, Signals, sin NgModules |
| Deploy | Render (back), Vercel (front), Supabase (PG São Paulo), n8n externo |

### Tests existentes

- **Backend:** `LeaderaApplicationTests` (sólo `contextLoads`), `LeadServiceTest` y `InteraccionServiceTest` con Mockito. **Cobertura estimada < 15%.**
- **Frontend:** archivos `*.spec.ts` autogenerados por Angular CLI, sin lógica real adentro. **Cobertura efectiva ~0%.**

---

## 3. Análisis por área

---

### [A] EXPERIENCIA DE USUARIO (UX/UI)

#### A.1 — Home diario bien diseñado, pero sin diferenciación entre "tarea hecha hoy" vs "lead atendido"

**Severidad: MEDIO**

El home (`home.html`) usa una progress bar y agrupa las tres secciones críticas (Prioritarios / Nuevos / Seguimientos), lo cual es excelente conceptualmente. Sin embargo:

- La barra de progreso mezcla "tareas completadas" con "leads que se contactaron hoy". Si un agente registra una interacción sobre un lead que no estaba en el listado de prioritarios/seguimientos del día, igual cuenta como "tarea cumplida".
- La sección **"Ya contactados hoy"** vive en otra pantalla (`/gestion-del-dia`). Para validar el progreso el agente tiene que salir del home.

**Impacto:** la métrica de progreso pierde precisión y el agente no tiene un cierre visual del día.

**Solución recomendada:**
- Mostrar el bloque "Ya contactados hoy" colapsado al final del home con su propio acordeón. Es 1 hora de trabajo y cierra el ciclo emocional del día.
- Separar el contador en "tareas planificadas hechas" vs "interacciones espontáneas registradas" para que el agente vea ambos.

#### A.2 — Modales construidos con `HTMLDialogElement` crudos, sin estado reactivo

**Severidad: MEDIO**

En `detalle-lead.ts:85,100,109,126,138,153` se manipulan modales recibiendo el elemento DOM por parámetro:

```typescript
agregarPropiedad(modal: HTMLDialogElement) { ... modal.close(); }
abrirModalContacto(modal: HTMLDialogElement) { ... modal.showModal(); }
```

**Impacto:**
- Acoplamiento DOM ↔ lógica de negocio.
- Difícil de testear (necesitás mockear elementos).
- Si en algún momento querés un modal accesible (focus trap, escape key, anuncios ARIA), tenés que reescribir todo.

**Solución recomendada:** envolver el estado de cada modal en un `signal<boolean>` y delegar el render al template con `@if`. Bonus: extraer un `<app-modal>` reutilizable. Esto te lleva ~2-3h y limpia 6+ páginas.

#### A.3 — Errores tratados de forma inconsistente

**Severidad: MEDIO**

Conviven tres patrones diferentes en los componentes:

```typescript
// detalle-lead.ts:67
this.errorGeneral.set(err.mensajeAmigable || 'No se pudo cargar el detalle del lead.')

// detalle-lead.ts:121
this.errorContacto = err.error || 'Ya existe un lead con ese teléfono o email.';

// nuevo-lead.ts:47
if (err.status === 409 || err.status === 403) { this.errorEmail = 'Ya existe...' }

// kanban-board.ts:91
alert('No se pudo cambiar el estado');
```

**Impacto:** UX impredecible. El `errorInterceptor` ya construye `mensajeAmigable`; usarlo de forma uniforme estandariza el feedback al agente.

**Solución recomendada:** crear un `NotificationService` (toast) que el interceptor inyecte y muestre. Eliminar todos los `alert()`.

#### A.4 — El sidebar es claro, pero faltan accesos directos a las acciones más frecuentes

**Severidad: BAJO**

El sidebar (`sidebar.html`) tiene Home, Gestión del día, Dashboard, Operaciones, Leads, Propiedades y Nuevo Lead. Falta:

- **FAB (botón flotante) "Registrar interacción"** en home y detalle de lead — es la acción más frecuente del día.
- **Buscador global de leads** (Cmd/Ctrl+K) — clave para un CRM con cartera de >50 leads.
- **Atajo "Llamar al próximo prioritario"** que abra el detalle del primer lead caliente sin contactar.

#### A.5 — Sin alertas ni recordatorios push

**Severidad: ALTO (para vender)**

El agente sólo se entera de leads pendientes si entra a la app. No hay:

- Push notifications (web push API).
- Mail diario con el resumen del día.
- Recordatorio cuando un lead caliente entra en "más de 7 días sin contacto".

**Impacto:** un CRM sin recordatorios es una agenda mejorada. La fricción de entrar todos los días es el principal motivo de churn en CRMs B2B chicos.

**Solución recomendada:** Bloque 3, prioridad alta. Web Push para los seguimientos del día + mail diario disparado desde n8n (workflow simple: GET `/leads/hoy` por agente → render template → SMTP).

#### A.6 — Excel export funcional pero pesado en main thread

**Severidad: BAJO**

`listar-leads.ts:82-98` exporta a Excel síncronamente. Hay un `setTimeout(..., 0)` para permitir el repaint, pero con >500 leads el thread se bloquea. Para >2000 leads conviene un WebWorker. Para ahora alcanza.

---

### [B] FUNCIONALIDADES Y LÓGICA DE NEGOCIO

#### B.1 — "Lead nuevo" se define sólo por `ultimoContacto IS NULL` — no diferencia origen vs contacto real

**Severidad: ALTO**

**Hallazgo:** En `LeadService.obtenerLeadsDeHoy` (línea 153) y la query `findByUltimoContactoIsNullAndAgenteEmailAndEstadoNot`, un lead es "nuevo" si nunca tuvo una `Interaccion` registrada. Esto coincide con la regla del `agents.md`, pero **rompe** en este escenario muy frecuente:

> "Llamé al lead apenas entró pero nunca registré la llamada en el sistema. Para el sistema sigue siendo nuevo."

**Impacto comercial:** el agente ve falsos pendientes y empieza a desconfiar del home.

**Solución recomendada:** agregar un campo `fechaPrimerContactoReal` en `Lead` (puede coincidir con `ultimoContacto` la primera vez), y un endpoint rápido "marcar como contactado sin crear interacción" para captura ágil. Migración pequeña, gran impacto en confianza del producto.

**Código actual** (`Lead.java:28-29`):
```java
private LocalDateTime fechaEntrada;
private LocalDateTime ultimoContacto;
```

**Código corregido sugerido:**
```java
private LocalDateTime fechaEntrada;             // cuándo cargó el lead
private LocalDateTime fechaPrimerContacto;      // primer toque real (manual o auto)
private LocalDateTime ultimoContacto;           // última interacción
```

Y en `findByUltimoContactoIsNullAndAgenteEmailAndEstadoNot` cambiar a `fechaPrimerContactoIsNullAndAgenteEmailAndEstadoNot`.

#### B.2 — Reclasificación de estado (CALIENTE/TIBIO/FRIO/INACTIVO) es 100% manual

**Severidad: MEDIO**

`agents.md` aclara que la reclasificación automática vive en n8n (fuera del backend). Esto **es una decisión válida** pero acarrea riesgos:

- Si n8n cae, el sistema queda sin reclasificación silenciosamente — sin alerta, sin log, sin nada.
- No hay un endpoint dedicado del backend para que n8n recategorice por bulk.

**Solución recomendada:**
1. Exponer `POST /leads/reclasificar-bulk` que reciba `[{ id, nuevoEstado, motivo }]` y registre el cambio con auditoría.
2. Backend hace fallback simple si no hay actividad de n8n en >24h (cron interno con Spring `@Scheduled`).

#### B.3 — Matching comprador → propiedad existe; matching propiedad → comprador es lo recién agregado

**Severidad: BAJO (cumple)**

`PropiedadService.buscarCompradoresPotenciales` (líneas 132-150) recorre operaciones de COMPRA del agente y filtra por zona (contains bidireccional), tipoVivienda, precio min/max, ambientes y metros. **Bien hecho**. Sólo dos detalles:

- El filtrado se hace **en memoria** (línea 147 `.filter(op -> coincide(...))`). Para un agente con 500 operaciones COMPRA abiertas es manejable; para 5000 conviene hacer pre-filtrado por zona en la query.
- No hay scoring de "qué tan bien matchea" — devuelve match o no-match. Un score (0-100) ayudaría a priorizar.

#### B.4 — Seguimientos no disparan ninguna notificación

**Severidad: ALTO**

Cuando se registra una interacción, el backend setea `fechaProximoSeguimiento = ahora + 3 días` (`InteraccionService:54`). Pero **nada** ocurre cuando esa fecha llega. El agente sólo se entera si entra al home ese día puntual.

**Impacto:** seguimientos perdidos = leads perdidos = revenue perdido.

**Solución recomendada:** workflow n8n cada mañana 8 AM → GET `/leads/hoy` por cada agente → envío de WhatsApp/mail "Tenés X leads para seguir hoy". Esto es 30 min de trabajo en n8n y vale oro comercialmente.

#### B.5 — Pipeline de operaciones está sólido

**Severidad: BAJO (positivo)**

`OperacionService.TRANSICIONES_VALIDAS` (líneas 41-47) define un state machine correcto:

```
ABIERTA → EN_GESTION → RESERVADA → CERRADA_GANADA (terminal)
   ↘           ↘            ↘
     CANCELADA (terminal desde cualquiera)
```

`validarConflictoConPropiedad` impide reservar/cerrar una propiedad si otra operación ya la tiene reservada/ganada. `sincronizarEstadoPropiedad` mantiene `EstadoPropiedad` consistente. **Esto está muy bien diseñado**.

Faltas menores:
- Una vez que una operación entra en `CERRADA_GANADA` o `CANCELADA` no se puede revertir. Para corregir errores humanos hace falta tocar la DB. Considerar permitir reabrir con auditoría.
- ALQUILER no tiene validaciones específicas (`agents.md` lo admite). Definir si necesita una `Busqueda` cuando es operación de "busco alquilar".

#### B.6 — Dashboard del agente: completo, pero algunas métricas son intensivas

**Severidad: MEDIO**

`DashboardService.obtenerDashboard` calcula snapshot, evolución (serie diaria), KPIs con delta vs período anterior, y agrupación por origen. **Es un dashboard muy completo** para el nivel del producto.

Problemas:
- `tiempoRespuestaPromedioDias` (línea 204) carga todos los leads del agente en memoria y por cada uno hace una query `findPrimeraInteraccion`. **N+1 puro**. Con 1000 leads → 1000 queries.
- El cache (`@Cacheable`) está sólo en `obtenerEstadisticasAgente` (`/stats`), no en `/dashboard`. El endpoint pesado no cachea.

**Solución:** mover el cálculo de "tiempo de respuesta" a una sola query SQL con `JOIN ... ORDER BY` y agregado, y cachear el dashboard con TTL 5 min y key compuesta `(agenteId, periodo)`.

#### B.7 — Faltan módulos clave para un CRM inmobiliario real

**Severidad: ALTO (para vender)**

Lo que LeadEra **no tiene hoy** y los competidores sí:

| Módulo | Impacto vs Excel | Esfuerzo |
|---|---|---|
| Documentos por operación (reserva, contrato, recibo) | Alto | M |
| Agenda con eventos sincronizables a Google Calendar | Alto | M |
| Plantillas de mensaje (WhatsApp/email) | Medio | S |
| Comisiones por operación y reportes mensuales | Alto | M |
| Cartera compartida entre agentes (multi-tenant inmobiliaria) | Alto | L |
| Tasaciones / valuaciones rápidas (link a propiedades comparables) | Medio | M |
| Importación masiva de leads desde portales | Alto | M |

---

### [C] ARQUITECTURA Y CALIDAD DE CÓDIGO

#### C.1 — Entidades JPA expuestas directamente en respuestas (viola tu propia regla)

**Severidad: ALTO**

`CLAUDE.md` dice explícitamente: *"DTOs separados de entities. Nunca exponer entity directamente en la API."* Sin embargo:

| Endpoint | Retorna entidad |
|---|---|
| `GET /leads/estado/{estado}` | `List<Lead>` |
| `GET /leads/sin-contactar` | `List<Lead>` |
| `GET /leads/{id}/interacciones` | `List<Interaccion>` |
| `GET /leads/inactivos` | `List<Lead>` |
| `GET /leads/prioritarios` | `List<Lead>` |
| `PUT /leads/{id}/estado` | `Lead` |
| `PATCH /leads/{id}/estado` | `Lead` |
| `PUT /leads/{id}/editar-contacto` | `Lead` (y recibe `Lead` en body) |
| `GET /leads/{leadId}/operaciones` (todos los métodos) | `Operacion` |
| `POST /leads/{leadId}/operaciones` | recibe **`Operacion`** en body (peligroso) |
| `GET /propiedades/lead/{leadId}` | `List<Propiedad>` |
| `GET /propiedades/{id}` | `Propiedad` |
| `POST /propiedades/lead/{leadId}` | recibe **`Propiedad`** en body |
| `POST /leads/{leadId}/operaciones/{id}/eventos` | recibe **`EventoOperacion`** en body |
| `LeadDetalleResponse` | contiene `List<Interaccion>` y `List<Propiedad>` (entidades) |

**Impacto:**
1. **Acoplamiento DB ↔ API**: agregar una columna en `Lead` cambia el contrato público.
2. **Riesgo de mass assignment**: el cliente envía un `Operacion` con `agente`, `fechaCierre`, `montoOperacion` que el service no necesariamente filtra. El service `crearOperacion` sobreescribe los campos críticos, pero es una defensa frágil.
3. **Lazy-loading leaks**: serializar entidades con relaciones puede traer accidentalmente colecciones grandes (`Lead.interacciones`).
4. **Las anotaciones `@JsonIgnore`/`@JsonIgnoreProperties` ya son síntoma**: estás peleando contra Jackson en lugar de usar el DTO correcto.

**Código actual** (`OperacionController.java:23-36`):
```java
@PostMapping
public ResponseEntity<Operacion> crearOperacion(
        @PathVariable Long leadId,
        @RequestBody Operacion operacion,        // ← acepta entidad cruda
        Authentication authentication
) {
    return ResponseEntity.ok(
            operacionService.crearOperacion(leadId, operacion, authentication.getName())
    );
}
```

**Código corregido:**
```java
@PostMapping
public ResponseEntity<OperacionResponseDTO> crearOperacion(
        @PathVariable Long leadId,
        @Valid @RequestBody CrearOperacionRequest request,
        Authentication authentication
) {
    Operacion creada = operacionService.crearOperacion(leadId, request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED).body(OperacionMapper.toResponse(creada));
}
```

Con un `CrearOperacionRequest` que tenga sólo `titulo`, `tipoOperacion`, `descripcion`, `propiedadId` (Long), `busqueda` (DTO también).

**Prioridad:** ALTA. Es trabajo de ~1-2 días, evita romper la API después.

#### C.2 — Mapper hecho a mano para Lead, ausente para el resto

**Severidad: MEDIO**

`LeadMapper` está bien. Falta `OperacionMapper`, `PropiedadMapper`, `InteraccionMapper`, `EventoMapper`. Considerar MapStruct (annotation processor, sin reflection en runtime) — agrega ~30s al build y evita ~300 líneas repetitivas.

#### C.3 — `obtenerInteraccionesPorId` admite explícitamente el bug

**Severidad: ALTO (Seguridad, ver D.2)**

```java
// LeadController.java:74-78
@GetMapping("/{id}/interacciones")
public List<Interaccion> obtenerInteraccionesPorId(@PathVariable Long id) {
    // Aquí podrías agregar seguridad también si quisieras
    return leadService.obtenerHistorialInteracciones(id);
}
```

**Esto es un bug conocido sin issue**. Cualquier agente autenticado puede leer interacciones de leads de otros agentes. Ver detalle en sección D.

#### C.4 — Inconsistencia entre `@PutMapping` y `@PatchMapping` en el mismo path

**Severidad: BAJO**

`LeadController` tiene `PUT /leads/{id}/estado` (cambia estado por query param) **y** `PATCH /leads/{id}/estado` (establece inactivo, sin body útil). Misma URL, dos semánticas:

```java
@PutMapping("/{id}/estado")    public Lead cambiarEstado(...)
@PatchMapping("/{id}/estado")  public ResponseEntity<Lead> establecerLeadInactivo(...)
```

**Impacto:** confuso para integradores y para n8n.

**Solución:** unificar en `PATCH /leads/{id}/estado` con body `{ "estado": "CALIENTE" | "INACTIVO" | ... }`.

#### C.5 — Variables MySQL-specific en queries

**Severidad: MEDIO (vas a migrar a PostgreSQL)**

`LeadRepository.countIngresosDelMes` y `OperacionRepository.countOperacionesGanadasDelMes` usan `MONTH(...)` y `YEAR(...)` con `CURRENT_DATE`. Estas funciones son JPQL estándar y funcionan en PG, **pero** no aprovechan los índices porque la función envuelve la columna. Usar rangos `>=` y `<` ya está hecho en `countByAgenteIdAndFechaEntradaGreaterThanEqual...`. Migrá las dos restantes al mismo patrón.

**Código actual:**
```java
@Query("SELECT COUNT(l) FROM Lead l WHERE l.agente.id = :agenteId " +
        "AND MONTH(l.fechaEntrada) = MONTH(CURRENT_DATE) " +
        "AND YEAR(l.fechaEntrada) = YEAR(CURRENT_DATE)")
long countIngresosDelMes(@Param("agenteId") Long agenteId);
```

**Corregido:**
```java
@Query("SELECT COUNT(l) FROM Lead l WHERE l.agente.id = :agenteId " +
        "AND l.fechaEntrada >= :inicioMes AND l.fechaEntrada < :inicioMesSiguiente")
long countIngresosDelMes(@Param("agenteId") Long agenteId,
                         @Param("inicioMes") LocalDateTime inicioMes,
                         @Param("inicioMesSiguiente") LocalDateTime inicioMesSiguiente);
```

Y calcular los bordes con `ZONA_ARGENTINA` en el service.

#### C.6 — Falta Flyway/Liquibase

**Severidad: ALTO (antes de prod estable)**

El proyecto tiene `spring.jpa.hibernate.ddl-auto=update` en dev y `validate` en prod. Los índices se aplican con un `.sql` manual (`db/leadera_indexes.sql`). Esto **no escala**:

- Si dos agentes desarrollan en paralelo y agregan columnas, no hay merge order.
- No hay rollback ni historial.
- `validate` en prod va a explotar la primera vez que alguien olvide correr el `.sql`.

**Solución:** sumar Flyway (dependencia única, ~5 min de setup). Mover el `.sql` actual a `V1__init_indexes.sql`. Para esto **necesitás autorización** (regla del CLAUDE.md, caso 4: nueva dependencia).

#### C.7 — Tests muy escasos

**Severidad: ALTO**

3 archivos de test, uno trivial (`contextLoads`). No hay tests de:
- Controllers (MockMvc).
- Seguridad (¿un agente accede a leads de otro?). **Justamente lo que tiene bug.**
- Transiciones de estado de operación.
- Matching de compradores.
- Frontend completo (los `.spec.ts` están vacíos).

**Solución:** mínimo de tests aceptable antes de vender:
- Smoke MockMvc para cada endpoint con/sin token.
- Tests de aislamiento por agente (3 tests por recurso).
- Test del state machine de operaciones.
- Test de matching con dataset fijo.

#### C.8 — `@Transactional` aplicado parcialmente

**Severidad: MEDIO**

`OperacionService.crearOperacion` y `aplicarCambioDeEstado` están `@Transactional` ✓. Pero `InteraccionService.crearInteraccion` modifica `Lead` y guarda `Interaccion` en dos saves separados **sin `@Transactional`**. Si el segundo save falla, queda inconsistencia (lead con `ultimoContacto` actualizado pero sin interacción asociada).

**Solución:** anotar `@Transactional` a nivel de clase en services que escriben.

---

### [D] SEGURIDAD

#### D.1 — 🔴 CRÍTICO: IDOR en endpoints de `/leads/agente/{id}/...`

**Severidad: CRÍTICO**

**Hallazgo:** `LeadController.java:108-123` expone tres endpoints que usan el `{id}` del path como identificador del agente **sin verificarlo contra el agente autenticado**:

```java
@GetMapping("/agente/{id}/stats")
public ResponseEntity<AgenteDashboardDTO> getStats(@PathVariable Long id) {
    return ResponseEntity.ok(leadService.obtenerEstadisticasAgente(id));
}

@GetMapping("/agente/{id}/actividad-reciente")
public ResponseEntity<List<ActividadRecienteDTO>> getActividadReciente(@PathVariable Long id) {
    return ResponseEntity.ok(leadService.obtenerActividadReciente(id, 5));
}

@GetMapping("/agente/{id}/dashboard")
public ResponseEntity<DashboardDTO> getDashboard(@PathVariable Long id, ...) {
    return ResponseEntity.ok(dashboardService.obtenerDashboard(id, periodo));
}
```

**Impacto:** Cualquier agente autenticado con un token válido puede ver el dashboard completo (leads ganados del mes, tasa de conversión, origen de leads, etc.) de **cualquier otro agente**, sólo cambiando el `id` en la URL. Es una violación grave de la regla de multi-tenancy declarada en `agents.md` ("Un agente no puede ver ni modificar datos de otro agente").

Esto se confirma con que `AuthService.getIdAgente()` en el frontend decodifica el id del JWT del lado del cliente, pero el backend nunca lo cruza.

**Código corregido:**
```java
@GetMapping("/agente/me/dashboard")
public ResponseEntity<DashboardDTO> getDashboard(
        Authentication authentication,
        @RequestParam(name = "periodo", defaultValue = "30d") String periodo) {
    Long agenteId = obtenerAgenteIdDesdeEmail(authentication.getName());
    return ResponseEntity.ok(dashboardService.obtenerDashboard(agenteId, periodo));
}
```

Misma estrategia para `/stats`, `/actividad-reciente`. **Eliminar** la versión con `{id}` o, si se necesita por compatibilidad, validar `id == agenteIdDelToken` y devolver `403` si no.

**Prioridad: parar todo y arreglar esto antes de seguir.**

#### D.2 — 🔴 CRÍTICO: IDOR en `GET /leads/{id}/interacciones`

**Severidad: CRÍTICO**

`LeadController.java:74-78` — el comentario en el código **admite el bug**:

```java
@GetMapping("/{id}/interacciones")
public List<Interaccion> obtenerInteraccionesPorId(@PathVariable Long id) {
    // Aquí podrías agregar seguridad también si quisieras
    return leadService.obtenerHistorialInteracciones(id);
}
```

`leadService.obtenerHistorialInteracciones` no filtra por agente. Cualquier agente autenticado puede leer las interacciones (incluido contenido de llamadas, notas internas) de leads ajenos.

**Solución:**
```java
@GetMapping("/{id}/interacciones")
public List<InteraccionDTO> obtenerInteraccionesPorId(@PathVariable Long id, Authentication auth) {
    return leadService.obtenerHistorialInteracciones(id, auth.getName());
}
```

Y en el service, replicar la verificación de `lead.getAgente().getEmail().equals(email)` como hacen el resto de los métodos. Devolver DTO, no entidad.

#### D.3 — 🔴 CRÍTICO: secrets de fallback hardcodeados en `application-dev.properties`

**Severidad: CRÍTICO**

```properties
# application-dev.properties:7-8, 18
spring.datasource.password=${DB_PASSWORD:Matecamilion2005}
app.jwt.secret=${JWT_SECRET:cambiar-este-secreto-en-prod-32-bytes-min-1234567890}
```

**Impacto:**
1. **Tu password personal de MySQL está en el repo público.** Cambiala YA en MySQL local.
2. Si en Render una env var no se inyecta correctamente (typo, fallo de plataforma), el sistema arranca con el secret de prueba — y cualquiera que conozca este repo puede forjar tokens válidos.

**Solución inmediata:**
```properties
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
```

Sin fallback. Si la env var no está, **Spring debe fallar al arrancar**. Después rotar las dos claves: la de MySQL (porque ya quedó en git history) y el `JWT_SECRET` de prod.

**Acción requerida del usuario:** confirmar que el password actual de MySQL no es el mismo en otras cuentas. Rotarlo. Considerar `git filter-repo` para borrar de la historia, aunque para repos privados el riesgo es menor.

#### D.4 — Token JWT almacenado en `localStorage`

**Severidad: ALTO**

`auth-service.ts:28-32` guarda el token en `localStorage`. Si la app sufre XSS (por ejemplo via `innerHTML` con datos de un lead manipulado), el token se exfiltra trivialmente.

**Solución recomendada (orden de menor a mayor esfuerzo):**
1. Sanitizar todo input de lead/propiedad/operación antes de renderizar (Angular ya lo hace por default con interpolación, pero verificá si usás `[innerHTML]` en algún lado).
2. Reducir tiempo de vida del token (de 24h a 1-2h) y agregar refresh token rotativo.
3. (Largo plazo) mover el JWT a cookies `httpOnly + SameSite=Lax + Secure`. Requiere reconfigurar CSRF.

#### D.5 — No hay refresh token

**Severidad: MEDIO**

El JWT vive 24h y no se puede revocar. Si un agente cambia password o se da de baja, los tokens viejos siguen siendo válidos hasta vencerse. No hay tabla de blacklist ni endpoint `/auth/refresh`.

**Solución:** implementar refresh token (con tabla en DB) y endpoint `/auth/refresh` que rote el access token cada 15 min, dejando el refresh válido 7 días con posibilidad de revocación.

#### D.6 — Rate limiting sólo cubre `/auth/login`

**Severidad: MEDIO**

`RateLimitFilter.java:23` chequea `LOGIN_PATH = "/auth/login"` y `POST`. **`/auth/register` no está protegido**, lo que permite:
- Spam de cuentas falsas para llenar la DB.
- Enumeración de emails registrados (vía `DuplicateResourceException`).

**Solución:** ampliar el filtro a un mapa de paths con sus capacidades:
```java
private static final Map<String, RateLimitConfig> PROTECTED_PATHS = Map.of(
    "/auth/login",    new RateLimitConfig(10, Duration.ofMinutes(1)),
    "/auth/register", new RateLimitConfig(5,  Duration.ofMinutes(10))
);
```

Y considerar limitar también `POST /leads` (5 leads por segundo por agente).

#### D.7 — Política de password débil

**Severidad: MEDIO**

`RegisterRequest.java:30-31`: `@Size(min = 6, max = 100)`. Sin requerir mayúsculas, números o símbolos. Una password de 6 caracteres alfabéticos es trivial.

**Solución:** subir mínimo a 10, regex que requiera al menos un dígito y una mayúscula. Bonus: integrar `zxcvbn` en el frontend para mostrar fuerza.

#### D.8 — Información sensible en logs (riesgo bajo, pero existe)

**Severidad: BAJO**

`JwtAuthenticationFilter.java:54,60,69` loguea el email del usuario en nivel `debug`. En prod el nivel default es `INFO`, así que no se ve. Pero si alguien activa debug, hay PII en los logs. Considerar truncar el email (`m****@gmail.com`) o moverlo a `trace`.

#### D.9 — CORS abierto a múltiples orígenes con comodines no implementados

**Severidad: BAJO**

`SecurityConfig.corsConfigurationSource` usa `setAllowedOriginPatterns(origins)` con valores que **vienen de `CORS_ALLOWED_ORIGINS` literal**. El comentario dice que sirve para Vercel preview deploys (`https://leadera-*.vercel.app`), pero hay que verificar que la env var en Render incluye explícitamente el patrón con `*`. Si no, los previews no funcionan o se está pasando un origen demasiado amplio.

**Acción:** revisar el valor exacto de `CORS_ALLOWED_ORIGINS` en Render y documentarlo.

#### D.10 — No hay 2FA / verificación por email

**Severidad: BAJO (hoy), MEDIO (al escalar)**

No hay confirmación de email al registrarse. Para B2B real es esperable. Postergable.

---

### [E] BASE DE DATOS Y PERFORMANCE

#### E.1 — N+1 en `LeadService.toResumenDTO`

**Severidad: ALTO**

Por cada lead del listado, se hacen 3 queries adicionales (`countByLeadIdAndTipo` × VENTA/COMPRA/ALQUILER):

```java
// LeadService.java:281-296
private LeadResumenDTO toResumenDTO(Lead lead) {
    long ventas = operacionRepository.countByLeadIdAndTipo(lead.getId(), TipoOperacion.VENTA);
    long compras = operacionRepository.countByLeadIdAndTipo(lead.getId(), TipoOperacion.COMPRA);
    long alquileres = operacionRepository.countByLeadIdAndTipo(lead.getId(), TipoOperacion.ALQUILER);
    ...
}
```

**Impacto:** paginación de 20 leads = 60 queries adicionales = ~60-150ms extra de latencia.

**Solución:** una sola query agrupada:

```java
@Query("SELECT o.lead.id, o.tipoOperacion, COUNT(o) " +
       "FROM Operacion o WHERE o.lead.id IN :leadIds " +
       "GROUP BY o.lead.id, o.tipoOperacion")
List<Object[]> contarOperacionesPorLead(@Param("leadIds") List<Long> leadIds);
```

Y armar un `Map<Long, EnumMap<TipoOperacion, Long>>` antes del stream. De 60 queries a 1.

#### E.2 — N+1 en `obtenerEstadisticasAgente.tiempoRespuesta`

**Severidad: ALTO**

```java
// LeadService.java:218-227
List<Lead> leads = leadRepository.findLeadsConFechaEntrada(agenteId);
double tiempoRespuesta = leads.stream()
        .mapToLong(lead -> {
            LocalDateTime primera = interaccionRepository.findPrimeraInteraccion(lead.getId());
            ...
        })
```

Una query por lead. Con 500 leads, 500 queries.

**Solución:** una sola query con agregado:
```java
@Query("SELECT l.id, MIN(i.fechaInteraccion), l.fechaEntrada " +
       "FROM Lead l LEFT JOIN l.interacciones i " +
       "WHERE l.agente.id = :agenteId AND l.fechaEntrada IS NOT NULL " +
       "GROUP BY l.id, l.fechaEntrada")
List<Object[]> findTiemposPrimerContacto(@Param("agenteId") Long agenteId);
```

#### E.3 — Cache `allEntries = true` evict afecta a todos los agentes

**Severidad: MEDIO**

`@CacheEvict(value = "estadisticasAgente", allEntries = true)` en cada `crearLead`, `cambiarEstado`, `establecerLeadInactivo`. Si el agente A crea un lead, **se invalidan las stats cacheadas del agente B, C, D, ...**.

El comentario del código explica que se usa `allEntries` porque no hay agenteId en scope sin una query extra. Pero la query extra cuesta ~1ms y es preferible:

**Solución:**
```java
@CacheEvict(value = "estadisticasAgente", key = "#root.target.obtenerAgenteIdPorEmail(#email)")
public LeadResponseDTO crearLead(CrearLeadRequest request, String email) { ... }
```

O más simple, pasar `agenteId` como parámetro a estos métodos desde el controller.

#### E.4 — Hibernate `ddl-auto=update` en dev

**Severidad: MEDIO**

`update` es seguro para agregar columnas, peligroso para renombrar (crea columna nueva y deja la vieja) y para tipo de datos (silenciosamente puede no hacer nada). Combinado con la ausencia de Flyway, las migraciones reales pasan por: "yo edito la entidad, lo que pase en MySQL local que la fuerza decida".

**Solución:** Flyway + `ddl-auto=validate` también en dev. Cuesta una migración inicial bien hecha.

#### E.5 — Faltan índices documentados en tablas críticas

**Severidad: MEDIO**

`db/leadera_indexes.sql` sólo cubre `leads`. Falta:
- `interaccion.lead_id` (debería existir por FK pero conviene confirmarlo).
- `interaccion.fecha_interaccion` (queries de dashboard).
- `operacion.agente_id`, `operacion.lead_id`, `operacion.propiedad_id`, `operacion.estado_operacion` (todas usadas en queries de listado y pipeline).
- `propiedad.lead_id`.
- `foto_propiedad.propiedad_id`.
- `evento_operacion.operacion_id`.

**Solución:** expandir el `.sql` con todos los índices necesarios, y migrarlo a Flyway.

#### E.6 — `findByAgenteEmailConInteracciones` sin paginar puede explotar

**Severidad: MEDIO**

```java
// LeadRepository.java:32-38
@Query("""
    SELECT DISTINCT l FROM Lead l
    LEFT JOIN FETCH l.interacciones
    WHERE l.agente.email = :email
    ORDER BY l.fechaEntrada DESC
""")
List<Lead> findByAgenteEmailConInteracciones(@Param("email") String email);
```

Sin paginación. Con un agente que tenga 2000 leads × 10 interacciones promedio = 20.000 filas materializadas. **Sí hay versión paginada justo abajo**, pero la `List` sigue siendo invocable. Eliminarla y forzar paginación.

#### E.7 — Conexiones Hikari muy bajas en prod

**Severidad: BAJO (consciente)**

`application-prod.properties:18-21` setea max 3 conexiones y min idle 1. Esto es **correcto para Render free tier + Supabase free**, pero hay que recordar subirlo al escalar. Anotarlo en un README de ops.

---

### [F] POTENCIAL DE AUTOMATIZACIÓN CON N8N

#### F.1 — `/leads/hoy` es el endpoint perfecto para automatizar

**Severidad: BAJO (positivo)**

Estructura limpia, agrupada por categorías, ya pensada para consumo externo. Un workflow n8n de "mail diario 8 AM" se arma en 15 min.

#### F.2 — Falta un endpoint para reclasificación masiva

**Severidad: ALTO**

El `agents.md` reconoce que la reclasificación CALIENTE/TIBIO/FRIO la hace n8n. Pero hoy n8n tiene que hacer 1 PUT por lead, lo que con 500 leads = 500 requests = 500 evicts de cache global.

**Solución:** crear `POST /admin/reclasificar-bulk` (con header `X-API-Key` o JWT especial) que reciba:
```json
[
  { "leadId": 123, "nuevoEstado": "TIBIO", "motivo": "60 días sin actividad" },
  ...
]
```

Y aplique en una sola transacción.

#### F.3 — Falta autenticación específica para n8n

**Severidad: MEDIO**

Hoy n8n tendría que loguearse con email/password como cualquier agente y guardar el JWT. Si el agente cambia su password, n8n se rompe.

**Solución:** crear API keys por agente (tabla `agente_api_keys`, header `X-API-Key: lk_live_xxx`). Cada workflow n8n usa su propia key. Revocable sin tocar la password del agente.

#### F.4 — Faltan webhooks salientes

**Severidad: MEDIO**

Cuando se crea un lead, se registra una interacción, o cambia un estado importante, **el backend no notifica a nadie**. n8n tendría que estar polleando.

**Solución:** sumar configuración de webhooks por agente (lista de URLs + eventos suscriptos: `lead.creado`, `interaccion.registrada`, `operacion.cerrada_ganada`). Disparar `POST` async después del commit.

#### F.5 — Endpoints útiles para automatización pero no expuestos

**Severidad: BAJO**

Lo que n8n querría tener:
- `GET /admin/leads/sin-contacto-hace?dias=30` (por agente o global del owner).
- `GET /admin/operaciones/estancadas?dias=15` (en EN_GESTION sin movimiento).
- `GET /admin/agentes/{id}/resumen-semanal` (mail dominical con cierre de semana).

Son endpoints simples de armar y disparan workflows comerciales reales.

#### F.6 — Logging insuficiente para auditoría

**Severidad: BAJO**

No hay tabla de `lead_audit` que registre cambios de estado, asignaciones, etc. Para un CRM B2B esto se pide tarde o temprano (qué pasó con cada lead, quién lo tocó).

---

## 4. Oportunidades de producto

### 🏆 Quick wins (alto impacto, bajo esfuerzo)

| # | Mejora | Esfuerzo | Impacto |
|---|---|---|---|
| 1 | **Mail diario con leads del día** (n8n + endpoint ya existe) | S (4h) | Alto: el agente entra a la app por inercia, no por costumbre |
| 2 | **WhatsApp click-to-chat** desde la card del lead (`wa.me/{telefono}`) | S (1h) | Altísimo en Mar del Plata |
| 3 | **Botón "Registrar llamada" en home** sin abrir el detalle | S (3h) | Reduce 3 clicks/día por agente |
| 4 | **Filtro "leads sin contactar hace X días"** en cartera | S (2h) | Recupera leads perdidos |
| 5 | **Estados visuales (badges con color)** consistentes en toda la app | S (3h) | Percepción de calidad |
| 6 | **Atajos de teclado** (`n` para nuevo lead, `/` para buscar, `g h` para home) | M (1d) | Diferencial vs Excel |
| 7 | **Botón "Llamar al próximo prioritario"** en home | S (2h) | Resuelve la pregunta "¿a quién llamo?" en 1 click |

### 🎯 Mejoras de UX clave

- **Onboarding 0**: cuando un agente entra por primera vez sin leads, mostrar un wizard "Importá tu primer lead desde WhatsApp" o "Pegá una planilla de Excel".
- **Estado vacío en cada pantalla**: hoy una cartera sin leads se ve rota. Diseñar empty states que inviten a la acción.
- **Confirmación visual fuerte** al cerrar una operación CERRADA_GANADA (animación, confeti, etc.). El agente vive de esos momentos.
- **Modo oscuro** — barato en CSS, esperado por el público.

### 🔗 Integraciones de alto valor

| Integración | Esfuerzo | Justificación comercial |
|---|---|---|
| **WhatsApp Business API** (Meta Cloud) | L (1-2 semanas) | El 95% del contacto inmobiliario en LATAM pasa por WhatsApp |
| **Google Calendar** (sync de seguimientos como eventos) | M (3-5d) | Cierra el loop con la agenda real del agente |
| **Meta Ads → leads** (webhook + API key) | M (2-3d) | Capta leads automáticamente del Facebook/Insta ads |
| **Importación ZonaProp/Argenprop** (scraping o CSV) | L | Cartera de propiedades sin recargar |
| **MercadoLibre Inmuebles** (API oficial) | L | Publicación directa |
| **n8n templates pre-armados** (mail diario, reclasificación, WA) | M | Diferenciador: "viene con automatizaciones de fábrica" |

### 🚀 Diferenciadores vs un Excel/CRM genérico

1. **"Tu día en una pantalla"** — el home como dashboard accionable.
2. **Matching automático** (ya tenés base): cuando subo una propiedad, me dice "tenés 4 compradores que la buscan".
3. **Recordatorios inteligentes** (caliente sin contactar = WhatsApp al agente automáticamente).
4. **Pipeline kanban con drag-and-drop** (ya está hecho — vale oro).
5. **Datos territoriales de Mar del Plata** (Barrios precargados, zonificación, precio promedio por zona). Esto es un diferencial regional difícil de copiar.

---

## 5. FODA técnico-comercial

### 🟢 Fortalezas

- **Arquitectura limpia y consistente**: separación controller/service/repository/dto, manejo global de excepciones, custom exceptions.
- **Aislamiento multi-tenant funcional en el 90% de los endpoints** (con las 2-3 excepciones críticas marcadas).
- **State machine de operaciones bien diseñado** (transiciones, sincronización con estado de propiedad, prevención de doble reserva).
- **Stack moderno y mantenido**: Spring Boot 3.3, Java 17, Angular 20 con Signals, sin NgModules.
- **Performance pensada**: cache de Caffeine, rate limiting de Bucket4j, índices documentados, queries con FETCH JOIN.
- **MVP deployado y funcionando** — pasaste el problema más difícil para un proyecto solo.
- **Funcionalidad core completa**: leads, interacciones, propiedades, operaciones, matching, dashboard, pipeline kanban.
- **Documentación interna sólida** (CLAUDE.md, agents.md, memory.md describen reglas de negocio claras).

### 🔴 Debilidades

- **3 IDORs activos** que invalidan la promesa de aislamiento multi-tenant.
- **Secrets en repo y password de DB personal expuesta** en git.
- **Entidades JPA expuestas en respuestas** — viola tu propia guía.
- **Tests ~0%** — imposible refactorizar con confianza.
- **Sin Flyway** — migraciones manuales con riesgo de divergencia entre entornos.
- **N+1 en endpoints calientes** (listado de leads, dashboard).
- **Sin notificaciones / recordatorios** — el agente sólo se entera si entra a la app.

### 🟡 Oportunidades

- **Mercado de Mar del Plata mal atendido**: la mayoría de inmobiliarias usa Excel + WhatsApp. Software argentino con conocimiento del mercado local es defensible.
- **WhatsApp Business API** abre el canal natural del oficio.
- **n8n + LeadEra** como combo: "tu CRM viene con automatizaciones preconfiguradas".
- **Pricing freemium**: hasta 50 leads gratis, después $X/mes. Bajo CAC, churn medible.
- **Vender el cuerpo de matching como producto separado**: una API que toma una propiedad y devuelve compradores potenciales puede licenciarse a inmobiliarias grandes.

### ⚠️ Amenazas

- **Competencia**: Tokko Broker, Nestoria CRM, RDStation existen y tienen marketing. La diferenciación tiene que ser foco + integraciones locales.
- **Dependencia de Render free tier**: 3 conexiones de DB y cold starts hacen que la primera carga del día sea lenta. Cualquier prospecto con prueba va a notarlo.
- **Cambios de WhatsApp Business API**: Meta sube costos y restringe acceso recurrentemente.
- **Regulación de datos personales (Ley 25.326)**: tenés que tener política de privacidad, retención y eliminación. Hoy no hay endpoint "borrar mi cuenta".
- **Riesgo de incident público si los IDORs se descubren** antes de fixearlos. Para un producto chico es prácticamente la muerte.

---

## 6. Scoring por área

Escala: 1-2 Crítico · 3-4 Deficiente · 5-6 Regular · 7-8 Bueno · 9-10 Excelente.

| Área | Score | Justificación |
|---|---|---|
| Experiencia de usuario (UX/UI) | **6.5** | Home bien pensado, navegación clara, pero faltan recordatorios, atajos y diseño de empty states. |
| Funcionalidades y lógica de negocio | **7.0** | Pipeline + matching + dashboard ya están. Falta diferenciación de origen vs contacto real, notificaciones, ALQUILER. |
| Arquitectura y calidad de código | **6.5** | Separación limpia, manejo global de errores, pero entidades expuestas, mapper único, sin migraciones. |
| **Seguridad** ⚖️ ×2 | **3.5** | 3 IDORs críticos + secrets en repo + localStorage. Bajísimo, **pesa doble**. |
| **Base de datos y performance** ⚖️ ×2 | **6.0** | N+1 en hot paths, cache global mal scopeado, faltan índices documentados en tablas no-`leads`. **Pesa doble**. |
| Potencial automatización n8n | **6.5** | `/leads/hoy` listo, pero faltan bulk endpoints, API keys y webhooks salientes. |

### Cálculo del score global ponderado

```
UX/UI:                      6.5 × 1.0 = 6.5
Lógica de negocio:          7.0 × 1.0 = 7.0
Arquitectura y código:      6.5 × 1.0 = 6.5
Seguridad (×2):             3.5 × 2.0 = 7.0
DB y performance (×2):      6.0 × 2.0 = 12.0
Automatización n8n:         6.5 × 1.0 = 6.5
                                       ----
                          Suma:        45.5
                          Pesos:        8.0
                                       ----
                          Promedio:     5.69
```

**Score global ponderado: 5.7 / 10 — Regular**

> El score global es duro **por la seguridad**. Sin los IDORs y los secrets en repo, el producto se mueve cerca de 6.8 (Bueno bajo). El gap entre "Regular" y "Bueno" se cierra con 3-5 días de trabajo concentrado.

---

## 7. Plan de acción priorizado

### 🔴 INMEDIATO (esta semana — bloquea cualquier venta)

| # | Tarea | Esfuerzo | Quien |
|---|---|---|---|
| 1 | **Fix IDOR en `/leads/agente/{id}/...`** — reemplazar `{id}` por `/me/` o validar contra el token. | 2h | Tú |
| 2 | **Fix IDOR en `GET /leads/{id}/interacciones`** — agregar validación de agente, devolver DTO. | 1h | Tú |
| 3 | **Quitar fallbacks de secrets de `application-dev.properties`** y rotar `DB_PASSWORD` + `JWT_SECRET` de prod. Rotar también la password de MySQL personal. | 1h | Tú |
| 4 | **Agregar validación de agente en `LeadRepository.findById`** vía un helper único `obtenerLeadDeAgente(id, email)` que centralice el check (reduce el riesgo de futuras omisiones). | 3h | Tú |
| 5 | **Smoke test: el agente A no puede ver datos del agente B** — un test de integración con MockMvc por cada endpoint sensible. | 1d | Tú |
| 6 | **Auditar `git log -p`** buscando si el password de DB apareció en commits anteriores; si sí, ejecutar `git filter-repo` y forzar repush. | 1h | Tú |

**Total: ~3 días.** Sin esto no se vende.

### 🟡 CORTO PLAZO (este mes)

| # | Tarea | Esfuerzo |
|---|---|---|
| 7 | **DTOs en todos los endpoints** que aún devuelven o reciben entidades JPA. Crear `OperacionMapper`, `PropiedadMapper`, `InteraccionMapper`. | 3-4d |
| 8 | **Fix N+1**: queries agregadas en `toResumenDTO` y `tiempoRespuestaPromedioDias`. | 1d |
| 9 | **Cache scopeado por agenteId** (eliminar `allEntries=true`) + cache del dashboard. | 0.5d |
| 10 | **Sumar Flyway** (requiere tu OK por dep nueva) y mover índices a `V1__init.sql`. | 1d |
| 11 | **Refresh token + reducir TTL del access a 1h.** | 2d |
| 12 | **Rate limit en `/auth/register` y `POST /leads`.** | 0.5d |
| 13 | **Notificación de errores frontend** unificada (toast) en lugar de `alert()` y signals dispersos. | 1d |
| 14 | **`fechaPrimerContactoReal` en `Lead`** + endpoint "marcar como contactado". | 1d |
| 15 | **Mail diario con `/leads/hoy`** (workflow n8n + SMTP). | 0.5d |
| 16 | **Tests de integración** para los flujos críticos (login, crear lead, registrar interacción, transiciones de operación, matching). Llevar cobertura backend a >40%. | 3-5d |
| 17 | **Política de password más fuerte** + zxcvbn en frontend. | 0.5d |
| 18 | **Endpoint admin bulk** para reclasificación n8n. | 0.5d |
| 19 | **WhatsApp click-to-chat** (`wa.me/`) en cards y detalles. | 2h |
| 20 | **Buscador global `Cmd+K`** en la app. | 1d |

**Total: ~3-4 semanas de trabajo concentrado.**

### 🟢 MEDIANO PLAZO (próximos 3 meses)

| # | Tarea | Esfuerzo |
|---|---|---|
| 21 | **WhatsApp Business API** (Meta Cloud) — envío de mensajes desde la app, registro automático de respuestas como interacciones. | 2-3 sem |
| 22 | **Google Calendar sync** — los seguimientos aparecen como eventos. | 1 sem |
| 23 | **Webhooks salientes** configurables por agente. | 1 sem |
| 24 | **API keys por agente** para n8n / integraciones externas. | 3d |
| 25 | **Documentos por operación** (subida a S3/Supabase Storage, link a reserva/contrato). | 1.5 sem |
| 26 | **Multi-tenant a nivel inmobiliaria** (agencia con múltiples agentes, líder ve agregados). | 2-3 sem |
| 27 | **Importación de leads** desde ZonaProp/Argenprop (CSV inicial, scraping/API después). | 2 sem |
| 28 | **Métricas avanzadas**: ROI por origen, costo por lead (si se conecta con Meta Ads). | 1.5 sem |
| 29 | **Comisiones por operación** con reporte mensual. | 1 sem |
| 30 | **Modo oscuro** + revisión de accesibilidad (WCAG AA mínimo). | 1 sem |
| 31 | **Audit log** de cambios sensibles en leads/operaciones. | 1 sem |
| 32 | **Auto-onboarding** con datasets de muestra para que un demo cliente vea el producto poblado. | 3d |

---

## 8. Conclusión ejecutiva

LeadEra es un MVP **técnicamente decente** que se acerca a la frontera entre "proyecto personal" y "producto vendible". La arquitectura es sana, el stack es moderno, el dominio de negocio está bien modelado, y las funcionalidades centrales (home diario, pipeline, matching, dashboard) **ya están construidas**.

Sin embargo, **hoy no se puede vender**. Tiene tres puertas abiertas que hacen que la promesa de "aislamiento por agente" sea falsa: dos IDORs en endpoints visibles desde el frontend y secrets de fallback hardcodeados que podrían dejar el sistema en estado inseguro si una env var falla en deploy. Estos tres ítems se resuelven en **3 días de trabajo concentrado**.

Después de eso, la prioridad cambia de "no romperse" a "diferenciarse". Los tres movimientos con mayor ROI son:

1. **Mail diario con los leads del día por agente** (4h). Trae al agente todos los días sin que tenga que recordar entrar.
2. **WhatsApp click-to-chat en todas las cards** (1h). El canal real del oficio en LATAM.
3. **Refactor a DTOs y fix de N+1** (1 semana). Te deja preparado para escalar sin reescribir.

Con la ruta inmediato → corto plazo ejecutada (~1 mes desde ahora), LeadEra estaría en un **score de ~7.2** (Bueno) y listo para vender a las primeras 5-10 inmobiliarias chicas de Mar del Plata como un producto pago de USD 15-30/mes por agente.

El techo del producto, si se ejecuta el bloque de mediano plazo (WhatsApp + Calendar + multi-tenant + integraciones con portales), está **claramente** en convertirse en el CRM inmobiliario por default de la ciudad. La barrera no es técnica: es ejecución y distribución.

**Próximos 3 pasos críticos en orden:**

1. Hoy: fixear los IDORs y rotar el `JWT_SECRET` y el password de DB.
2. Esta semana: smoke tests de aislamiento por agente.
3. Próxima semana: empezar el mail diario por n8n y los DTOs en respuestas que aún devuelven entidades.

---

*Fin del reporte.*
