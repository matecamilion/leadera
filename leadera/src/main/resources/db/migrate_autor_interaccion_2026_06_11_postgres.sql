-- ============================================================
-- Migración manual: autor de cada interacción (Fase 1 de la jerarquía
--                   DUENO -> AGENTE -> ASISTENTE)
--                   (PostgreSQL / Supabase — PROD)
-- Fecha: 2026-06-11
--
-- Cambios:
--   - Nueva columna `autor_id` en `interaccion`: FK nullable a agente(id).
--     Registra el agente REAL que cargó la interacción (para un asistente,
--     él mismo; no su supervisor, que sigue siendo el dueño del lead).
--   - Backfill: las interacciones previas a esta fase no tienen autor, así
--     que se les asigna el agente dueño de su lead (leads.agente_id), que es
--     el mejor dato disponible de quién la hizo antes de existir asistentes.
--
-- Nombres reales verificados en el código:
--   * tabla interaccion, FK interaccion.lead_id -> leads.id
--   * tabla leads (NO "lead": evita la palabra reservada de Postgres),
--     FK leads.agente_id -> agente.id
--
-- IMPORTANTE:
--   * HACER BACKUP EN SUPABASE ANTES DE EJECUTAR
--     (Dashboard > Database > Backups, o pg_dump).
--   * Ejecutar este script contra la base ANTES de deployar el backend con
--     los cambios (prod usa ddl-auto=validate y no levanta si falta la
--     columna). El proyecto no usa Flyway ni Liquibase.
--   * Todo corre dentro de UNA transacción: si algo falla se revierte
--     completo y la base queda como estaba.
-- ============================================================

BEGIN;

-- 1) Columna autor, nullable (las viejas se backfillean abajo; las nuevas
--    las setea el backend en cada alta de interacción).
ALTER TABLE interaccion ADD COLUMN autor_id BIGINT NULL;

-- 2) FK a agente.
ALTER TABLE interaccion
    ADD CONSTRAINT fk_interaccion_autor
    FOREIGN KEY (autor_id) REFERENCES agente(id);

-- 3) Índice para resolver "interacciones por autor".
CREATE INDEX idx_interaccion_autor ON interaccion (autor_id);

-- 4) Backfill: cada interacción vieja queda atribuida al agente dueño de su lead.
UPDATE interaccion i
SET autor_id = l.agente_id
FROM leads l
WHERE i.lead_id = l.id
  AND i.autor_id IS NULL;

COMMIT;

-- ============================================================
-- VERIFICACIÓN (correr después del COMMIT):
--
-- La columna debe existir y ser nullable (is_nullable = 'YES'):
--   SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--   WHERE table_name = 'interaccion' AND column_name = 'autor_id';
--
-- Tras el backfill, ninguna interacción con lead asociado debe quedar sin autor.
-- Debe dar 0 (solo podrían quedar NULL las que tuvieran lead_id NULL, que no aplica):
--   SELECT count(*) FROM interaccion WHERE autor_id IS NULL AND lead_id IS NOT NULL;
--
-- Control de coherencia: el autor backfilleado coincide con el dueño del lead.
-- Debe dar 0:
--   SELECT count(*)
--   FROM interaccion i JOIN leads l ON l.id = i.lead_id
--   WHERE i.autor_id <> l.agente_id;
-- ============================================================
