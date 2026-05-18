# agents.md — Contexto para Claude Code y n8n

## Sobre mí

Soy un desarrollador freelance junior, sin experiencia laboral formal todavía. Estoy aprendiendo mientras construyo. Me gusta que me expliquen el razonamiento detrás de cada decisión técnica — no solo el "qué" sino el "por qué". Estoy abierto a que me enseñes mientras trabajamos juntos.

**Cómo quiero que trabajes conmigo:**
- Cuando no tenés suficiente contexto para tomar una decisión, **proponé opciones** con una explicación clara de cada una. No asumas ni te frenes.
- Explicá lo que estás haciendo mientras lo hacés, especialmente si toca lógica crítica.
- Si algo puede hacerse mejor o de forma más limpia, decímelo aunque no te lo haya pedido.

### memory.md — Aprendizaje acumulativo

Existe un archivo `memory.md` en la raíz del proyecto. **Tenés que mantenerlo actualizado** a lo largo del tiempo.

Cada vez que ocurra alguna de estas situaciones, agregá una entrada al `memory.md`:
- Te corrijo algo (técnico, de estilo, de comportamiento)
- Tomamos una decisión de arquitectura o diseño
- Descubrimos un error o algo que no debe repetirse
- Aprendés algo nuevo sobre mis preferencias o forma de trabajar
- Cambia algo importante sobre el proyecto

**Formato de cada entrada en `memory.md`:**
```
## [fecha] — [título corto]
**Tipo:** corrección | decisión | preferencia | error a evitar | cambio en el proyecto
**Detalle:** descripción clara de lo aprendido
```

> No esperés que te lo pida. Si aprendiste algo nuevo, actualizá el `memory.md` solo.

---

## El proyecto: Leadera

CRM inmobiliario que ayuda a agentes inmobiliarios a gestionar su día: saber a quién contactar, registrar interacciones, guardar propiedades y operaciones de compra/venta.

- **Frontend:** https://leadera-tawny.vercel.app/
- **Estado:** MVP deployado y funcionando

### Stack técnico

| Capa | Tecnología | Deploy |
|---|---|---|
| Backend | Spring Boot (Java) | Render (free tier) |
| Frontend | Angular 20 | Vercel |
| Base de datos | Supabase (PostgreSQL) | Servidor São Paulo |

### Estructura del repositorio

```
leadera/                  ← monorepo raíz
├── backend/              ← Spring Boot
└── frontend/             ← Angular 20
```

### Convenciones de arquitectura

**Backend:**
- Entidades JPA con Lombok: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Repositorios con queries JPQL anotadas
- DTOs: `LeadResumenDTO` para listados (liviano), entidad completa para detalle
- Excepciones custom: `ResourceNotFoundException`, `UnauthorizedActionException`, `BadRequestException`
- CORS habilitado para `localhost:4200` en todos los controllers
- Zona horaria: `America/Argentina/Buenos_Aires` — usar siempre `LocalDateTime.now(ZONA_ARGENTINA)`

**Frontend:**
- Angular standalone components con Signals (`signal()`, `computed()`)
- Sin NgModules

---

## Reglas de negocio críticas

### 1. Sistema de "Leads del día" (Home diario)

El home es el núcleo operativo. Tiene 4 categorías:

| Categoría | Condición |
|---|---|
| **Nuevos** | `ultimoContacto IS NULL` y `estado ≠ INACTIVO` |
| **Prioritarios** | `estado = CALIENTE` y `ultimoContacto` hace más de 7 días |
| **Seguimientos del día** | `fechaProximoSeguimiento` entre inicio y fin del día actual, `estado ≠ INACTIVO` |
| **Ya contactados hoy** | `ultimoContacto` en el día actual |

> ⚠️ Un lead es "nuevo" solo si **nunca tuvo una interacción registrada**. La nota de creación o descripción de origen no cuenta como contacto.

### 2. Registro de interacciones

Al crear una interacción (`POST /leads/{id}/interacciones`), el backend ejecuta automáticamente:
- Actualiza `lead.ultimoContacto` con la fecha de la interacción
- Si no se especifica `fechaProximoSeguimiento` → se asignan **+3 días automáticamente**
- Si se especifica fecha → se respeta

**Tipos disponibles (enum `TipoInteraccion`):**
`LLAMADA`, `WHATSAPP`, `EMAIL`, `VISITA`, `REUNION`, `NOTA`, `SEGUIMIENTO`

> Después de registrar una interacción, el frontend redirige a `/gestion-del-dia`.

### 3. Estados del lead

| Estado | Significado |
|---|---|
| `CALIENTE` | Urgencia de compra, listo para cerrar |
| `TIBIO` | Mostró interés, en evaluación |
| `FRIO` | Sin contacto previo o interés bajo |
| `INACTIVO` | Descartado manualmente por el agente |

> El estado se cambia **manualmente** por el agente. No hay reclasificación automática en el backend (eso lo hace n8n por fuera).
> Inactivar un lead tiene su propio endpoint dedicado: `/estado/inactivo`.

### 4. Operaciones: tipos y validaciones

| Tipo | Requisito |
|---|---|
| `VENTA` | Requiere una `Propiedad` asociada que pertenezca al mismo lead |
| `COMPRA` | Requiere una `Busqueda` con `tipoVivienda` y `zona` obligatorios (se crea al momento) |
| `ALQUILER` | Sin restricciones especiales aún |

- Toda operación nace con estado `ABIERTA`
- `fechaCreacion` en zona horaria Argentina
- Un agente solo puede ver/crear operaciones de sus propios leads

### 5. Relación Propiedad → Lead → Operación

```
Lead (vendedor)
  └── Propiedad (la que quiere vender)
        └── Operacion VENTA
              └── estado: ABIERTA → EN_GESTION → RESERVADA → CERRADA_GANADA
```

Para `COMPRA`, en lugar de propiedad existe una `Busqueda` con: tipo de vivienda, zona, precio mínimo, precio máximo, ambientes, metros.

> ⚠️ No se puede vincular una propiedad de otro lead a una operación.

### 6. Unicidad de leads por agente

Por agente, no pueden existir dos leads con el mismo `telefono` ni con el mismo `email`. Aplica tanto en creación como en edición.

### 7. Seguridad y multi-tenancy

- Autenticación via JWT. El email del agente se extrae del token en el backend.
- **Toda query crítica filtra por `agente.email` o `agente.id`.**
- Un agente no puede ver ni modificar datos de otro agente.
- Las operaciones validan que `lead.agente.email == emailAgente` antes de cualquier acción.

### 8. Estadísticas del agente (`/perfil`)

Calculadas en tiempo real: activos, calientes/tibios/fríos, ganados del mes, nuevos del mes, perdidos, interacciones últimos 7 días, tasa de conversión (ganados/nuevos del mes).

---

## Contexto n8n

Estoy aprendiendo n8n. Por ahora lo uso para automatizaciones externas que complementan el backend, como la reclasificación automática de estados de leads. Soy principiante — explicá los nodos, la lógica de los workflows y las decisiones de diseño con claridad.

---

## Lo que no debe romperse

- La lógica del home diario y sus 4 categorías
- El efecto automático de registrar una interacción sobre `ultimoContacto` y `fechaProximoSeguimiento`
- El aislamiento por agente (multi-tenancy)
- El uso de `ZONA_ARGENTINA` en cualquier cálculo de fechas
- Las validaciones de operaciones por tipo (VENTA necesita propiedad del mismo lead, COMPRA necesita búsqueda)
