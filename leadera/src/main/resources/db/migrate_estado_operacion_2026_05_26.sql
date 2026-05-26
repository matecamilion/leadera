-- ============================================================
-- Migración manual: actualización del ENUM `estado` en `operaciones`
-- Fecha: 2026-05-26
--
-- Cambios:
--   - Se elimina el valor EN_GESTION
--   - Se agregan PUBLICADA y EN_NEGOCIACION
--
-- IMPORTANTE: ejecutar este script contra la base ANTES de levantar
-- el backend con los cambios. El proyecto no usa Flyway ni Liquibase.
-- ============================================================

-- 1) Reasignar filas con el estado viejo. EN_GESTION ≈ PUBLICADA
--    (ambos representan la operación activa en el mercado tras
--    la apertura). Si en tu caso querés mapearlas a EN_NEGOCIACION
--    cambialo manualmente acá.
UPDATE operaciones SET estado = 'PUBLICADA' WHERE estado = 'EN_GESTION';

-- 2) Redefinir el ENUM con los 6 valores nuevos en el orden correcto.
ALTER TABLE operaciones
MODIFY COLUMN estado ENUM(
    'ABIERTA',
    'PUBLICADA',
    'RESERVADA',
    'EN_NEGOCIACION',
    'CERRADA_GANADA',
    'CANCELADA'
);
