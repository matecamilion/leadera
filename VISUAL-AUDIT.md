# VISUAL-AUDIT — LeadEra frontend

> Auditoría estética de `leadera-front/` — 2026-07-21.
> Método: revisión inline de los 36 archivos CSS + templates, más el detector determinístico de la skill impeccable (56 hallazgos: 44 "overused-font" descartados como falso problema en UI de producto, 8 `transition: width`, 3 `border-left` accent, 1 falso positivo de imagen).
> Solo diagnóstico: **ningún archivo de la app fue modificado.**
> Referencia de calidad interna: `listar-leads`. Referencia externa: dashboards tipo Linear/Stripe.

---

## 0. Corrección al supuesto del brief

Los tokens del redesign **no** están definidos por componente: son **globales**, en `src/styles.css :root`:

```css
--color-bg:           #FBF8F4;  /* arena */
--color-card:         #FFFFFF;
--color-text:         #16213B;  /* navy ink */
--color-text-muted:   #7C7568;  /* warm gray */
--color-border:       #EAE3D8;  /* arena oscuro */
--color-primary:      #0F6E5C;  /* teal */
--color-primary-dark: #0B5246;
--color-caliente:     #D85A30;  /* ember */
--color-tibio:        #EF9F27;  /* amber */
--color-frio:         #378ADD;
--color-inactivo:     #888780;
--font-display:       'Inter', ...;
```

`perfil.css` es el único que construye un sistema completo encima (aliases `--ink/--brand/--hot` + variantes soft + `--shadow-sm/md`), con el comentario "Tokens oficiales Leadera". **Ese bloque es el mejor candidato a sistema definitivo** — hoy está atrapado en el `:host` de un solo componente.

Lo que sí está fragmentado por componente: sombras, radios, focus rings, botones, badges y el `font-family` repetido ~30 veces.

---

## 1. Diagnóstico por componente

Criterio: **Redesign** = usa `var(--color-*)` como base. **Legacy** = 0 usos de tokens, paleta Tailwind hardcodeada (emerald `#10b981`, teal-400 `#14b8a6`, blue `#2563eb`, slate `#f8fafc/#64748b/#0f172a`). **Mixto** = tokens + hardcodes conviviendo.

### Páginas (`src/app/pages/`)

| Componente | Estado | `var(--color-*)` | Problemas concretos (archivo:selector) |
|---|---|---|---|
| `perfil` | **Redesign** ✦ | 10 + aliases | El mejor. Pero: `monospace` genérico en 14 selectores (`.pipe-monto`, `.funnel-num`, `.activity-time`…) cae en Courier y desentona; `border-left: 3px` accent en `perfil.css:214`; emoji 👋 en `.wave` (perfil.html:47) |
| `detalle-operacion` | Mixto (mayoría redesign) | 62 | `#f8fafc` ×7 en fondos; badges con hardcodes (`#fee2e2`, `#fef3c7`, `#dbeafe`) que duplican el sistema de `styles.css`; ✓ como texto en el stepper (detalle-operacion.html:78-111); 📞 en :195 |
| `detalle-lead` | Mixto (mayoría redesign) | 46 | Slate residual (`#f8fafc` ×6, `#475569` ×4); modal con sombra pesada `0 25px 50px -12px rgba(0,0,0,.25)`; ✕ como texto para cerrar error (detalle-lead.html:4) |
| `listar-leads` | Mixto — **vara de calidad en layout, no en tokens** | 37 | Toda la piel slate hardcodeada (`.contenedor-listado` color `#1e293b`, `.operacion-mini-card` bg `#f8fafc` borde `#e2e8f0`); `@import` de Inter duplicado (listar-leads.css:2); `border-left: 3px` en :284; `.pestañas-filtro` con ñ en el nombre de clase |
| `propiedad-detalle` | Mixto (mayoría redesign) | 36 | Ámbar/rojo/verde Tailwind en badges (`#fef3c7` ×4, `#d97706` ×4, `#16a34a`); 📞 en :109 |
| `nueva-interaccion` | **Redesign** | 31 | El más limpio después de perfil. Sin hardcodes de color (0 hex) |
| `leads-seccion` | Mixto | 24 | **Indigo `#4f46e5`** como acento (única página con ese color); grises Tailwind gray (`#6b7280`, `#111827`) en vez de slate o tokens |
| `propiedad-editar` | **Redesign** | 13 | Menor: `#f1f5f9` y `#b91c1c` sueltos |
| `home` | Mixto | 10 | `.highlight` en **`#14b8a6`** (teal-400 legacy ≠ `--color-primary`) — home.css:29, es lo primero que se ve; título con `clamp(1.8rem, 5vw, 2.8rem)` (fluid type no corresponde en producto); chips propios (`.chip-caliente` `#fee2e2/#b91c1c`) que duplican badges globales con otros valores; `.resumen-label` `#94a3b8` a 0.7rem → contraste ~2.9:1, **falla WCAG AA** |
| `kanban-board` | Mixto-legacy | 5 | Acento azul `#2563eb` + teal `#14b8a6` conviviendo; grises gray (`#374151`) vs slate del resto |
| `gestion-del-dia` | **Legacy** | 0 | Emerald `#10b981`; `h1` a **2.8rem** (era tamaño display para Fraunces, en Inter plano queda gigante) — gestion-del-dia.css:44; radios 24px y **32px** (sobre-redondeado); ☕ emoji (gestion-del-dia.html:24) |
| `mis-tareas` | **Legacy** | 0 | Emerald ×10 como primario; `border-left: 3px solid #ef4444` (mis-tareas.css:404); 🎉 emoji (mis-tareas.html:32); modal sombra `0 20px 50px rgba(0,0,0,.25)` |
| `mi-equipo-agente` | **Legacy** | 0 | Emerald ×9; focus ring emerald `rgba(16,185,129,.1)`; modal sombra pesada idem |
| `mi-asistente` | **Legacy** | 0 | Emerald; focus ring emerald |
| `equipo` | **Legacy** | 0 | Emerald ×5; modal sombra pesada |
| `mi-perfil` | **Legacy** | 0 | Emerald; **`'JetBrains Mono'` sin importar** (mi-perfil.css:85 — cae al monospace del sistema); focus ring `rgba(52,211,153,.1)` (emerald-400, distinto al de las demás) |
| `propiedades-lead` | **Legacy** | 0 | Teal-400 `#14b8a6` ×10 + azul `#2563eb`; sombra teñida `0 6px 20px rgba(20,184,166,.12)`; `font-family: 'Inter'` repetido en 7 selectores |
| `operaciones-por-estado` | **Legacy** | 0 | `#14b8a6` como acento; fondo `#f8fafc` en `:host` (≠ arena `--color-bg`) |
| `operaciones-lead` | **Legacy** | 0 | Azul + teal-400 mezclados; badges hardcodeados que duplican los globales |
| `operaciones-cerradas` | **Legacy** | 0 | Azul `#2563eb`; verde `#15803d` ad-hoc |
| `propiedades` | **Legacy** | 0 | Azul `#2563eb` como acento |
| `gestionar-busqueda` | **Legacy** | 0 | Azul; **sombras azules pesadas** `0 10px 15px -3px rgba(37,99,235,.2)` en botones (delata no-profesional); radio 24px |
| `nuevo-lead` | **Legacy** | 0 | Azul `#2563eb`; rojo `#e53e3e` (≠ rojos del resto); radio 24px |
| `interacciones-lead` | **Legacy** | 0 | Azul `#2563eb` ×4 |
| `auth/login` | **Legacy** | 0 | Emerald `#10b981`; h1 **32px** (register y cambiar-password usan 28px) |
| `auth/register` | **Legacy** | 0 | Emerald; rojo `#ef4444` |
| `auth/cambiar-password` | **Legacy** | 0 | Emerald |

### Shared + app (`src/app/shared/`, `src/app/`)

| Componente | Estado | Problemas |
|---|---|---|
| `sidebar` | Mixto (mayoría redesign) | Estructura buena (activo `#E6F0EE`+teal). Slate residual (`#475569`, `#f1f5f9`, `#1e293b`); **sin labels de sección** (el brief pide grupos con labels chicos uppercase); `z-index` mágicos 999/1000/1100 |
| `header` | **Legacy** | Verde `#32b389` que no existe en ninguna paleta (ni token ni Tailwind); `#15803d`; ✓ como texto (header.html:12) |
| `lead-card` | **Legacy caótico** | Colores fuera de todo sistema: `#f74d4d`, `#ef7d84`, `#f4df7b`, `#fff3bf` — cuarta versión de los badges de temperatura; radio 22px |
| `lead-section` | Mixto | 16 vars pero badges propios (`#fde8e0`, `#f5c2ad`…) — tercera versión de badges; ⚠ como texto (lead-section.html:10) |
| `toast-container` | **Legacy** | Emerald/azul/rojo genéricos; sin animación de entrada/salida definida ahí |
| `fotos-propiedad` | **Legacy** | Emerald; overlay `rgba(0,0,0,.5)` con sombra `0 20px 60px` |
| `footer` (`app.css`) | **Legacy** | `#14b8a6` |

**Score global: 4 redesign · 9 mixtos · 19 legacy** (de 32 superficies con estilos).

### Problemas transversales

1. **Tres acentos primarios compitiendo**: teal token `#0F6E5C` (redesign), emerald `#10b981` (9 archivos), azul `#2563eb` (8 archivos), más teal-400 `#14b8a6` e indigo `#4f46e5` sueltos. Un usuario navegando ve el botón primario cambiar de color según la página → sensación inmediata de producto cosido.
2. **Cuatro focus rings distintos**: emerald `.1`, emerald-400 `.1`, azul `.05/.1` a 4px, teal `.08/.1`.
3. **Cuatro sistemas de badge de temperatura**: `styles.css` (oficial), `home.css` chips, `lead-section.css`, `lead-card.css` — cuatro paletas distintas para el mismo dato de dominio.
4. **≥8 implementaciones de botón primario** (`.btn-exportar-excel`, `.btn-ld-primary`, `.btn-guardar`, `.btn-guardar-evento`, `.pe-btn-guardar`, `.btn-refresh`…), cada una con su padding/radio/hover.
5. **Tipografía sin escala**: h1 de página en 1.35 / 1.4 / 1.6 / 1.75 / 2.8rem / clamp / 28px / 32px. Post-Fraunces, los tamaños display (2.8rem) quedaron huérfanos.
6. **Monospace sin criterio**: `monospace` genérico (perfil, 14 usos), `'JetBrains Mono'` no importado (mi-perfil). Para cifras alcanza con Inter + `font-variant-numeric: tabular-nums`.
7. **Radios de 4 a 32px** sin escala; pills alternan `99px`/`999px`.
8. **Sombras de 5 recetas**: sutil `0 1px 3px`, media, pesada de modal `.25`, teñidas de color (azul, teal), y las `--shadow-sm/md` de perfil (las correctas).
9. **Emojis/símbolos de texto en UI**: 📞 ☕ 🎉 👋 y ✓ ✕ ⚠ como caracteres — habiendo Material Symbols ya cargado.
10. **`font-family` redeclarado ~30 veces** + un `* { font-family }` global en `styles.css` que pisa todo (hace inútiles las redeclaraciones y complica usar otra fuente donde sí se quiera).
11. **Fondo de página inconsistente**: arena `#FBF8F4` (token) vs slate `#f8fafc` hardcodeado en ~12 páginas — el usuario ve el fondo "cambiar de temperatura" al navegar.
12. **Accesibilidad**: `#94a3b8` sobre blanco en textos chicos (labels de home, `.chip-via`) ≈ 2.9:1, bajo el mínimo 4.5:1.

---

## 2. Sistema unificado propuesto

Principio: **promover el bloque de `perfil.css` a `styles.css :root`** (ya se autodenomina "tokens oficiales") y completarlo. Todo lo nuevo es aditivo — no rompe nada existente.

```css
:root {
  /* ── Color (ya existen) ── */
  --color-bg: #FBF8F4;  --color-card: #FFFFFF;  --color-border: #EAE3D8;
  --color-text: #16213B;  --color-text-muted: #7C7568;
  --color-primary: #0F6E5C;  --color-primary-dark: #0B5246;
  --color-caliente: #D85A30;  --color-tibio: #EF9F27;
  --color-frio: #378ADD;  --color-inactivo: #888780;

  /* ── Tinta secundaria (desde perfil.css) ── */
  --ink-2: #2D3A35;          /* texto secundario fuerte */
  --ink-3: var(--color-text-muted);
  --ink-4: #AAA49C;          /* solo decorativo, nunca texto <14px */

  /* ── Superficies soft (desde perfil.css) ── */
  --brand-soft: #E6F0EE;   --brand-softer: #EDF6F3;
  --hot-soft:   #FDEEE8;   --warm-soft:    #FEF5E0;
  --cool-soft:  #E8F1FC;

  /* ── Sombras (desde perfil.css; matan las 5 recetas) ── */
  --shadow-sm: 0 1px 0 rgba(22,33,59,.04), 0 1px 2px rgba(22,33,59,.04);
  --shadow-md: 0 1px 0 rgba(22,33,59,.04), 0 6px 24px -8px rgba(22,33,59,.08);
  --shadow-modal: 0 8px 40px -8px rgba(22,33,59,.18);

  /* ── Radios ── */
  --r-sm: 6px;    /* chips, badges cuadrados */
  --r-md: 10px;   /* inputs, botones */
  --r-lg: 14px;   /* cards */
  --r-pill: 999px;

  /* ── Focus ring único ── */
  --focus-ring: 0 0 0 3px rgba(15, 110, 92, .12);

  /* ── Tipografía ── */
  --font-ui: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  /* --font-display ya apunta a Inter; mantener como alias de --font-ui */
}
```

**Escala tipográfica fija** (registro producto: rem fijos, sin clamp):

| Rol | Tamaño | Peso |
|---|---|---|
| Label/eyebrow | 0.72rem, uppercase, tracking .05em | 600 |
| Meta/caption | 0.8rem | 400–500 |
| Body/tabla | 0.875rem | 400 |
| Body fuerte / nav | 0.92rem | 500–600 |
| h2 de card | 1rem | 600 |
| **h1 de página (única)** | **1.6rem** | **600** |
| KPI | 2rem–2.4rem, `font-variant-numeric: tabular-nums`, letter-spacing -0.02em | 600–700 |

Reglas de aplicación:
- **Un solo acento**: teal para acción primaria, selección y estado activo. Emerald, azul, teal-400 e indigo desaparecen. El azul queda solo como `--color-frio` (dato de temperatura), nunca como acción.
- **Semánticos**: éxito = teal (no emerald), error = `#B42318` único, warning = amber token.
- **Fondo de página**: siempre `var(--color-bg)` arena. `#f8fafc` muere.
- **Texto**: mínimo `--ink-3` para tamaños <0.875rem; `#94a3b8` prohibido como color de texto.
- **Cifras**: Inter + `tabular-nums`; se elimina `monospace`/JetBrains.
- **Clases utilitarias globales en `styles.css`**: `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-ghost`, `.btn-danger` (una sola definición de botón); el sistema de badges Fase-1 ya existente se completa y las 3 copias locales migran a él.
- **`* { font-family }` → `body { font-family: var(--font-ui) }`** y se borran las ~30 redeclaraciones.

---

## 3. Plan por fases

Cada fase es deployable sola a main (cambios CSS + HTML mínimos, sin tocar lógica `.ts` salvo donde se indica clase en template). Orden = impacto visual ÷ esfuerzo.

---

### Fase 1 — Fundamento: tokens globales + limpieza de emojis
**Archivos**: `src/styles.css`, `src/app/pages/perfil/perfil.css`, + 8 templates HTML (solo líneas de emoji).
**Qué cambia**:
- Se agregan a `:root` los tokens de la sección 2 (aditivo).
- `perfil.css` pasa a consumir los globales (borra su copia local, mantiene los aliases cortos si se quiere).
- `* { font-family }` → `body { }`.
- Emojis fuera: 📞 → icono `call` de Material (detalle-operacion.html:195, propiedad-detalle.html:109), ✓ del stepper → icono `check`, ✕ → icono `close`, ⚠ → icono `warning`, ☕ 🎉 👋 se eliminan.
**Qué NO se toca**: ningún color de páginas todavía; ningún `.ts`.
**Riesgo**: bajo. Tokens nuevos no afectan nada hasta usarse; cambio de `*`→`body` puede destapar algún elemento de formulario sin fuente heredada (los `<button>/<input>` no heredan por defecto — mitigación: `button, input, select, textarea { font: inherit }` en la misma fase).

### Fase 2 — Auth: primera impresión (login, register, cambiar-password)
**Archivos**: `auth/login/login.css`, `auth/register/register.css`, `auth/cambiar-password/cambiar-password.css`.
**Qué cambia**: emerald → `var(--color-primary)`; focus ring → `var(--focus-ring)`; h1 unificado a un solo tamaño en las tres; fondo → `var(--color-bg)`; sombra de card → `--shadow-md`.
**Qué NO se toca**: estructura HTML, validaciones, lógica.
**Riesgo**: mínimo. Son 3 archivos chicos, find-replace de colores, pantallas aisladas del resto.

### Fase 3 — Superficie diaria: home + sidebar + header
**Archivos**: `pages/home/home.css`, `shared/sidebar/sidebar.css` (+`sidebar.html` para labels de sección), `shared/header/header.css`.
**Qué cambia**:
- home: `.highlight` `#14b8a6`→teal; h1 `clamp`→1.6rem fijo (o 2rem si querés jerarquía de "saludo", pero fijo); chips `.chip-*` → badges globales de `styles.css`; slate → tokens; `.resumen-label` `#94a3b8`→`--ink-3`.
- sidebar: labels de grupo uppercase chicos ("PRINCIPAL", "LEADS", "OPERACIONES"…), slate residual → tokens, escala z-index comentada.
- header: `#32b389`/`#15803d` → tokens; ✓ → icono.
**Qué NO se toca**: rutas, items de navegación, lógica de contadores.
**Riesgo**: medio-bajo. El agrupado del sidebar toca HTML (solo `<span>` de labels); el resto es CSS. Es la fase de mayor impacto percibido: home+sidebar+header están en pantalla el 100% del tiempo.

### Fase 4 — Familia tareas/gestión: mis-tareas, gestion-del-dia, mi-asistente
**Archivos**: los 3 CSS homónimos.
**Qué cambia**: emerald → teal; `h1` 2.8rem → 1.6rem (gestion-del-dia); radios 24/32px → `--r-lg`; `border-left` rojo de mis-tareas.css:404 → fondo tinte `--hot-soft` + borde completo; sombras de modal → `--shadow-modal`; botones → clases `.btn-*` (toca clases en HTML).
**Qué NO se toca**: lógica de tareas/notificaciones.
**Riesgo**: medio. Migrar botones a clases globales implica editar templates; hacerlo componente por componente y verificar visual.

### Fase 5 — Familia operaciones: operaciones-lead, operaciones-cerradas, operaciones-por-estado
**Archivos**: los 3 CSS homónimos.
**Qué cambia**: azul/teal-400 → tokens; badges locales → sistema global (los nombres de clase ya coinciden con `styles.css` en gran parte); fondo `:host` `#f8fafc` → `--color-bg`; verde `#15803d` → teal.
**Riesgo**: bajo. Mayormente find-replace; los badges globales ya existen para operación.

### Fase 6 — Familia propiedades: propiedades, propiedades-lead, fotos-propiedad
**Archivos**: `propiedades.css`, `propiedades-lead.css`, `components/fotos-propiedad/fotos-propiedad.css`.
**Qué cambia**: `#2563eb`/`#14b8a6`/emerald → tokens; sombra teñida teal → `--shadow-md`; los 7 `font-family` de propiedades-lead se borran (heredan de body tras Fase 1); paginación `.btn-pagina` → `.btn-ghost`.
**Riesgo**: bajo-medio (propiedades-lead es grande, ~500 líneas).

### Fase 7 — Equipo y perfil de usuario: equipo, mi-equipo-agente, mi-perfil
**Archivos**: los 3 CSS homónimos.
**Qué cambia**: emerald ×17 → teal; `'JetBrains Mono'` → Inter + `tabular-nums`; focus rings → token; modales → `--shadow-modal`.
**Riesgo**: bajo. Find-replace + un selector de fuente.

### Fase 8 — Formularios: nuevo-lead, gestionar-busqueda, interacciones-lead
**Archivos**: los 3 CSS homónimos.
**Qué cambia**: azul → teal; sombras azules de botón (gestionar-busqueda) → `--shadow-sm`; rojo `#e53e3e` → error único; radios 24px → `--r-lg`; inputs → focus ring token.
**Riesgo**: bajo. Formularios ya usan reactive forms; solo piel.

### Fase 9 — Cards compartidas y kanban: lead-card, lead-section, leads-seccion (+ kanban-board, toast-container)
**Archivos**: `shared/lead-card/lead-card.css`, `shared/lead-section/lead-section.css`, `pages/leads-seccion/leads-seccion.css`, `pages/kanban-board/kanban-board.css`, `shared/toast-container/toast-container.css`.
**Qué cambia**: **matar las 3 copias de badges de temperatura** → sistema global (lead-card `#f74d4d/#f4df7b`, lead-section `#fde8e0/#f5c2ad`, y chips restantes); indigo `#4f46e5` → teal; grises gray → tokens; toasts → teal/error únicos con `--shadow-md`.
**Qué NO se toca**: drag & drop del kanban.
**Riesgo**: medio. lead-card/lead-section se renderizan dentro de varias páginas — verificar en todas (home, gestion-del-dia, leads-seccion).

### Fase 10 — Detalles y pulido final: detalle-lead, detalle-operacion, propiedad-detalle (+ listar-leads)
**Archivos**: los 4 CSS homónimos.
**Qué cambia**: slate residual → tokens en los tres detalles (ya son mayoría redesign); badges hardcodeados → globales; sombra de modal → token; en listar-leads: borrar `@import` duplicado de Inter, slate → tokens, renombrar `.pestañas-filtro`→`.pestanas-filtro` (clase con ñ), `border-left` → tratamiento sin stripe.
**Riesgo**: bajo por archivo, pero son las páginas más densas — ir de a una, listar-leads al final (es la vara: solo alinear tokens, no rediseñar).

---

## 4. Top 5 quick wins

1. **Auth emerald → teal** (Fase 2 completa): 3 archivos chicos, es la primera pantalla que ve cualquier usuario/demo, y hoy tiene un color primario que no existe en el resto del producto. ~15 min.
2. **`home.css:29` `.highlight: #14b8a6` → `var(--color-primary)`**: una línea; el título de la pantalla principal deja de usar un teal que no es el de la marca.
3. **Emojis fuera de la UI** (📞 ☕ 🎉 👋 + ✓✕⚠ de texto → Material Symbols ya cargados): ~10 líneas en 8 templates; es el tell #1 de "no profesional".
4. **`gestion-del-dia.css:44` h1 2.8rem → 1.6rem + radios 32px → 14px**: dos valores; la página más "gritona" del sistema pasa a escala.
5. **Focus ring y sombra de modal unificados** (agregar `--focus-ring` y `--shadow-modal` a `:root` y reemplazar las 4 variantes de focus + las 3 sombras `.25`): mejora transversal de "sensación de sistema" con un find-replace acotado.

---

## 5. Pendientes fuera de alcance de esta auditoría

- Breadcrumbs: no existen en ninguna página (el brief los pide); requiere decisión de UX + componente nuevo → proponer como feature aparte, no como fase de restyling.
- Empty states: 10 componentes tienen el suyo; unificar como componente compartido es refactor de HTML/TS, no de piel.
- `transition: width` en progress bars (8 casos): funciona, pero anima layout; migrar a `transform: scaleX()` es pulido de performance, no estético.
- Skeleton loaders para estados de carga (hoy hay spinners/nada).
- Accesibilidad completa (focus visible en todo, aria): auditar en el Bloque 2/4 del plan general del proyecto.
