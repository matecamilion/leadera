# Leadera — Roadmap hacia la primera venta como SaaS

**Fecha:** 2026-06-15  
**Enfoque elegido:** B — Seguridad + Billing en paralelo, ritmo sostenido  
**Modelo comercial:** SaaS mensual por inmobiliaria, Stripe como procesador de pagos  
**Equipo:** 1 desarrollador (solo)  
**Mercado objetivo:** Inmobiliarias de Mar del Plata (Argentina)  
**Contexto competitivo:** Usan Excel/Google Sheets + Tokko Broker (publicaciones, no leads). Leadera captura los leads que Tokko genera.

---

## Resumen ejecutivo

Leadera tiene arquitectura sólida (multi-tenant, JWT, roles, Spring Boot + Angular 20) y funciona en producción. El gap para venderlo es: 3 vulnerabilidades críticas de seguridad abiertas, sin billing, sin landing page, y sin onboarding autónomo. Las Fases 0→2 cierran ese gap. Las Fases 3→5 construyen crecimiento sostenible.

---

## Fase 0 — Seguridad Crítica

**Duración estimada:** ~1 semana  
**Prerequisito:** Obligatorio antes de que entre cualquier cliente real.

### Entregables

| Item | Severidad | Esfuerzo | Detalle |
|------|-----------|----------|---------|
| Fix cross-tenant write via entity binding | CRÍTICO | S | `PropiedadController` y `OperacionController` aceptan entidades con `id` en el body — un `merge()` puede sobrescribir datos de otro tenant. Reemplazar con DTOs + lookup explícito por el agente autenticado. |
| Supabase Storage: eliminar INSERT anónimo | CRÍTICO | M | El bucket `fotos-propiedades` permite subida sin autenticación (rol `anon`). Activar RLS en Supabase, usar `service_role` key solo en backend, nunca en frontend. |
| environment.ts dev → localhost:8080 | MEDIO | S | `environment.ts` apunta a producción (`onrender.com`) en lugar de `localhost:8080`. Cualquier prueba local toca prod. |
| Remover email del log WARN | MEDIO | S | `JwtAuthenticationFilter.java:71` loguea el email del usuario en nivel WARN. Viola Ley 25.326 de Protección de Datos Personales (Argentina). Cambiar a nivel DEBUG o eliminar. |
| CORS: restringir wildcard vercel.app | MEDIO | S | `allowedOriginPatterns` con `leadera-*.vercel.app` permite cualquier proyecto Vercel con ese prefijo. Enumerar orígenes exactos en variable de entorno. |

### Gate de salida
Sin vulnerabilidades críticas abiertas. El primer cliente puede entrar sin riesgo de exposición de datos de otro tenant.

---

## Fase 1 — Billing con Stripe

**Duración estimada:** ~3 semanas  
**Resultado:** Un usuario puede registrarse, elegir un plan y pagar sin intervención manual.

### Definición de planes (propuesta inicial, ajustable)

| Plan | Precio/mes | Límites | Target |
|------|-----------|---------|--------|
| Básico | $X ARS | 1–3 agentes, sin asistentes | Inmobiliaria unipersonal |
| Pro | $Y ARS | 4–10 agentes, asistentes habilitados | Inmobiliaria mediana |
| Agencia | $Z ARS | Ilimitado | Franquicia / multi-sucursal |

### Entregables backend (Spring Boot)

- Integración Stripe SDK: crear `Customer`, crear `Subscription`, manejar `PaymentIntent`
- Entidad `Suscripcion` o columnas en `Inmobiliaria`: `stripe_customer_id`, `stripe_subscription_id`, `plan`, `estado_suscripcion`, `trial_fin`
- Webhook handler (`/stripe/webhook`): `invoice.payment_succeeded`, `invoice.payment_failed`, `customer.subscription.deleted`, `customer.subscription.updated`
- Migración DB: columnas de suscripción en `inmobiliaria`
- Guard de acceso: si `estado_suscripcion = INACTIVO` o trial expirado → respuestas 402 en endpoints protegidos

### Entregables frontend (Angular)

- Pantalla de selección de planes (pricing page dentro del app, post-registro)
- Integración Stripe.js / Stripe Checkout (redirect a Stripe Checkout o embedded)
- Trial de 14 días sin tarjeta: acceso completo, banner de cuenta regresiva
- Pantalla "Tu prueba venció" con CTA a planes
- Manejo de 402 en `errorInterceptor` → redirigir a pantalla de billing

### Webhooks críticos a implementar

| Evento Stripe | Acción en Leadera |
|---------------|-------------------|
| `checkout.session.completed` | Activar suscripción, marcar trial como convertido |
| `invoice.payment_succeeded` | Renovar período activo |
| `invoice.payment_failed` | Enviar email de alerta, dar gracia de 3 días |
| `customer.subscription.deleted` | Desactivar acceso (estado INACTIVO) |

### Gate de salida
Un usuario nuevo puede registrarse → elegir plan → pagar con tarjeta → acceder al CRM. Sin intervención manual.

---

## Fase 2 — Landing Page + Onboarding

**Duración estimada:** ~2 semanas  
**Resultado:** Canal de adquisición propio. Un prospect puede encontrar Leadera, entender la propuesta de valor y activar el trial sin asistencia.

### Entregables

| Item | Esfuerzo | Detalle |
|------|----------|---------|
| Landing page externa | L | Separada del app Angular. Propuesta de valor, casos de uso (inmobiliarias con Tokko), planes, CTA a trial gratuito. Stack sugerido: Next.js o Astro en Vercel. |
| Onboarding wizard (in-app) | M | Post-registro: paso a paso para configurar nombre de inmobiliaria, cargar primer agente, tour de la app. Reduce tiempo-a-valor. |
| Email transaccional | S | Resend / SendGrid: bienvenida, "tu trial vence en 3 días", "tu pago fue procesado", "problema con tu pago". |
| Flujo recuperar password | S | No existe actualmente. Generar token temporal, envío por email, pantalla de reset. |
| Dominio propio | S | `leadera.app` u otro. Apuntar frontend Vercel y backend Render a dominio custom. SSL automático en ambos. |

### Propuesta de valor para la landing (borrador)

> "Usás Tokko para publicar tus propiedades. Usá Leadera para no perder los leads que llegan. Pipeline de seguimiento, historial completo, equipo coordinado. Empezá gratis 14 días."

### Gate de salida — Gate comercial ★
Primera inmobiliaria de Mar del Plata pagando. A partir de acá el roadmap cambia de modo "construir" a modo "crecer".

---

## Fase 3 — Deuda Técnica Prioritaria

**Duración estimada:** ~2 semanas  
**Cuándo:** Después de la primera venta real. No bloquea la venta pero sí bloquea la escala.

### Entregables

| Item | Esfuerzo | Impacto |
|------|----------|---------|
| Flyway: baseline de migraciones | M | Reemplaza los 7 scripts SQL manuales por migraciones versionadas. Sin Flyway, cada nuevo deploy en un ambiente nuevo es riesgo de error humano. |
| Fix N+1 en stats del dueño | S | `LeadService.obtenerEstadisticasAgente` hace 1 query/lead. Refactor a método batch existente. |
| Fix paginación en memoria | S | Queries con `JOIN FETCH` + `Page` materializan todo en RAM. Separar en 2 queries (ids paginados + fetch de colecciones). |
| Lazy loading en Angular | M | 24 rutas eager-loaded = bundle inicial pesado. Implementar `loadComponent` en `app.routes.ts`. |
| JWT expiry configurable | S | Hardcodeado a 24h en `JwtService.java:46`. Externalizar a `application.properties`. |
| Tests OperacionService | M | La máquina de estados de operaciones (la lógica más crítica del negocio) no tiene tests. Agregar cobertura de transiciones válidas e inválidas. |

---

## Fase 4 — Features Diferenciadores

**Duración estimada:** ~4–6 semanas  
**Cuándo:** Con 1–3 clientes pagando. Estos son los features que hacen que no puedan volver a Excel.

### Entregables (por impacto comercial)

| Feature | Esfuerzo | Impacto | Descripción |
|---------|----------|---------|-------------|
| Import leads desde ZonaProp / MercadoLibre Inmuebles | L | ★★★★★ | Integración vía API oficial de MercadoLibre (avisos) o email parsing de notificaciones de ZonaProp → lead automático en Leadera. Elimina el paso manual de cargar el lead. Diferenciador único vs Excel. |
| Notificaciones WhatsApp Business | L | ★★★★★ | Envío de mensajes de seguimiento a leads desde Leadera vía Twilio / Meta Business API. El canal de comunicación principal en MdP. |
| Recordatorios automáticos de tareas | M | ★★★★☆ | Email/push cuando vence una tarea. Hoy las tareas existen pero sin notificaciones. |
| Reporte PDF de cierre de operación | M | ★★★☆☆ | PDF con resumen de la operación, propiedades visitadas, línea de tiempo, datos de las partes. Para entregar al cliente en el cierre. |
| Dashboard de métricas avanzadas | M | ★★★★☆ | Tasa de conversión por agente, tiempo promedio de cierre, leads por canal, embudo de conversión. Para el dueño. |

---

## Fase 5 — Escala y Operación

**Duración estimada:** Ongoing  
**Cuándo:** Con 5+ clientes pagando.

### Entregables

| Item | Esfuerzo | Descripción |
|------|----------|-------------|
| Panel de admin (tuyo) | L | Dashboard interno para ver todos los tenants, MRR, estado de suscripciones, churn, uso. Sin esto operás a ciegas. |
| Refresh tokens | M | Hoy los tokens duran 24h fijos. Implementar refresh tokens para sesiones continuas sin re-login. |
| Integración Tokko Broker | M | API de Tokko para sincronizar propiedades bidireccional. Profundiza la complementariedad. |
| Documentación Swagger/OpenAPI | S | Swagger UI en `/api-docs`. Útil si abrís API a integraciones externas. |
| Docker Compose dev local | S | Un solo `docker-compose up` levanta backend + DB + frontend. Reduce fricción de onboarding de nuevos colaboradores. |

---

## Decisiones abiertas

| Decisión | Opciones | Recomendación |
|----------|----------|---------------|
| Precio de los planes | A definir según conversaciones con clientes | Arrancar con precio bajo (validación), subir con datos reales |
| Landing page stack | Next.js / Astro / Webflow | Astro si querés SSG puro y rápido, Webflow si querés cero código para la landing |
| Email transaccional | Resend / SendGrid / Postmark | Resend: DX excelente, plan gratis generoso, integración simple |
| WhatsApp | Twilio / Meta Business API directo / 360dialog | Twilio: más caro pero mejor DX. 360dialog: más económico para volumen. |
| Importación de portales | API oficial (si existe) / scraping / email parsing | ZonaProp tiene API limitada. MercadoLibre tiene API de avisos. Evaluar disponibilidad. |

---

## Stack tecnológico (sin cambios de arquitectura)

- **Backend:** Spring Boot 3.3 + Java 17 + PostgreSQL (Supabase) + Render
- **Frontend:** Angular 20 standalone + Vercel
- **Billing:** Stripe (subscriptions + webhooks)
- **Storage:** Supabase Storage (con RLS habilitado post-Fase 0)
- **Email:** Resend (recomendado) o SendGrid
- **Dominio:** A definir

---

## Criterios de éxito por fase

| Fase | Criterio de éxito |
|------|------------------|
| 0 | 0 vulnerabilidades críticas abiertas |
| 1 | Un usuario completa registro + pago sin ayuda |
| 2 | Primera inmobiliaria de MdP pagando ★ |
| 3 | Sin queries N+1 en el happy path del dueño |
| 4 | Al menos 1 feature de Fase 4 en producción con feedback de cliente |
| 5 | Panel de admin operativo, churn < 10% mensual |
