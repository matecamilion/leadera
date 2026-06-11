-- ============================================================
-- Migración manual: jerarquía DUENO -> AGENTE -> ASISTENTE
--                   (PostgreSQL / Supabase — PROD)
-- Fecha: 2026-06-11
--
-- Cambios:
--   - Nueva columna `supervisor_id` en `agente`: self-FK nullable.
--     Solo el rol ASISTENTE la usa (apunta al AGENTE que supervisa);
--     DUENO y AGENTE quedan en NULL.
--
-- El rol ASISTENTE NO requiere cambio de schema: la columna `rol` es
-- VARCHAR(20) con @Enumerated(STRING), así que el nuevo valor entra como
-- texto. No hay ENUM nativo ni CHECK sobre `rol` que haya que tocar.
--
-- IMPORTANTE:
--   * HACER BACKUP EN SUPABASE ANTES DE EJECUTAR
--     (Dashboard > Database > Backups, o pg_dump).
--   * Ejecutar este script contra la base ANTES de deployar el backend
--     con los cambios (prod usa ddl-auto=validate y no levanta si falta
--     la columna). El proyecto no usa Flyway ni Liquibase.
--   * Todo corre dentro de UNA transacción: si algo falla se revierte
--     completo y la base queda como estaba.
-- ============================================================

BEGIN;

-- 1) Columna self-referencing, nullable (DUENO/AGENTE quedan en NULL).
ALTER TABLE agente ADD COLUMN supervisor_id BIGINT NULL;

-- 2) FK contra la propia tabla agente.
ALTER TABLE agente
    ADD CONSTRAINT fk_agente_supervisor
    FOREIGN KEY (supervisor_id) REFERENCES agente(id);

-- 3) Índice para resolver el árbol de supervisión (asistentes por agente).
CREATE INDEX idx_agente_supervisor ON agente (supervisor_id);

COMMIT;

-- ============================================================
-- VERIFICACIÓN (correr después del COMMIT):
--
-- La columna debe existir y ser nullable (is_nullable = 'YES'):
--   SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--   WHERE table_name = 'agente' AND column_name = 'supervisor_id';
--
-- Tras la migración todos quedan en NULL (aún no hay ASISTENTEs):
--   SELECT count(*) FROM agente WHERE supervisor_id IS NOT NULL;  -- debe dar 0
-- ============================================================
