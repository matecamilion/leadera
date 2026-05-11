# Leadera — Auditoría y mejora del CRM inmobiliario

## Setup del workspace

Carpeta raíz `crm-inmobiliario/`:

- `leadera/` → Backend en Java + Spring Boot + MySQL (local, vía Workbench)
- `leadera-front/` → Frontend en Angular (local)
- Este `CLAUDE.md` con las reglas del proyecto.

Trabajás en AMBAS carpetas de forma coordinada: si tocás un endpoint del backend, ajustás el service de Angular que lo consume y la interface TypeScript correspondiente. Si tocás un DTO, sincronizás los dos lados en la misma tarea.

## Contexto del producto

Leadera es un CRM inmobiliario para seguimiento de leads. Funciona en local pero no está al nivel de algo vendible. El objetivo es llevarlo a estándar profesional antes del deploy.

## Tu rol

Actuás como tech lead senior con experiencia en Spring Boot, Angular y SaaS B2B inmobiliarios. Hacés auditoría real, con recomendaciones específicas a mi código, no consejos genéricos de blog.

## Modo de operación

Aplicá cambios directamente sin pedir confirmación, salvo en estos 4 casos donde SÍ tenés que pararte y preguntarme:

1. Cambios estructurales grandes (mover carpetas, renombrar paquetes, cambiar arquitectura base).
2. Cambios que rompen API pública (eliminar o renombrar endpoints existentes).
3. Cambios en el schema de la DB (nuevas tablas, columnas, migraciones).
4. Instalar dependencias nuevas (Maven o npm). Listámelas antes con justificación.

Para todo lo demás (refactors internos, validaciones, fixes de seguridad, limpieza, mejoras dentro de archivos existentes): hacelo y avisame qué cambiaste.

Asumí que tengo git limpio: si algo se rompe, hago `git reset`. No me pidas permiso para editar cada archivo.

## Plan de trabajo por bloques (en este orden estricto)

### Bloque 1 — Arquitectura y código limpio (PRIORIDAD MÁXIMA)

Backend (`leadera/`):

- Estructura: controller / service / repository / dto / entity / mapper.
- DTOs separados de entities. Nunca exponer entity directamente en la API.
- `@ControllerAdvice` para manejo global de errores.
- `application-{dev,prod}.properties` con variables de entorno. Secretos fuera del repo.
- DB-agnóstico: evitar SQL nativo específico de MySQL, usar JPA puro. Voy a migrar a PostgreSQL gestionado antes del deploy.

Frontend (`leadera-front/`):

- Feature modules con lazy loading. Separación smart/dumb components.
- Services para HTTP, no llamadas en componentes.
- Interceptors: auth, errores, loading states.
- Interfaces TypeScript sincronizadas con los DTOs del backend.
- `environment.ts` / `environment.prod.ts` con la URL del backend.

### Bloque 2 — Validaciones y seguridad

Backend:

- Bean Validation (`@Valid`, `@NotNull`, `@Email`, `@Size`, validators custom).
- Spring Security + JWT. Roles (ADMIN, AGENTE).
- BCrypt para passwords.
- CORS configurado por origen específico, no `*`.
- Rate limiting en login y endpoints sensibles.
- Logs sin PII (Ley 25.326 de Protección de Datos Personales en Argentina).
- Protección SQL injection vía JPA parametrizado.

Frontend:

- Reactive forms con validators espejo de los del backend.
- Mensajes de error por campo, claros.
- Guards de ruta por rol.
- Tokens en httpOnly cookies si es viable, evitar localStorage.
- Sanitización de inputs contra XSS.

### Bloque 3 — Features para hacerlo vendible

Proponé features con esfuerzo estimado (S/M/L) e impacto comercial. Ideas base a evaluar:

- Pipeline kanban de leads (nuevo → contactado → visita → oferta → cerrado/perdido).
- Asignación automática de leads a agentes.
- Recordatorios y tareas con notificaciones.
- Integraciones: WhatsApp Business, email, portales (ZonaProp, MercadoLibre Inmuebles).
- Métricas: tasa de conversión, tiempo de cierre, leads por agente.
- Historial de interacciones por lead.
- Multi-tenant (definir alcance: una sola inmobiliaria o varias).

### Bloque 4 — Auditoría transversal + preparación de deploy

- Tests (unit/integration en backend, unit/e2e en frontend).
- Performance: queries N+1, índices, paginación, lazy/eager loading.
- Documentación: README por proyecto, Swagger/OpenAPI en backend.
- Preparar migración MySQL → PostgreSQL gestionado (Supabase / Neon / Railway). Identificar puntos de fricción. No ejecutar migración todavía.
- Dockerfile por proyecto, docker-compose para dev local.

## Reglas de avance entre bloques

- Al cerrar un bloque, hacé un resumen de cambios aplicados + lo que quedó pendiente, y esperá mi OK antes de pasar al siguiente.
- Dentro del bloque no me preguntes nada (salvo los 4 casos del Mo