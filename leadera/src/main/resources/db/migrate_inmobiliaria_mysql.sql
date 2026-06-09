-- ============================================================
-- Migración manual: multi-tenant por Inmobiliaria (MySQL — DEV)
-- Fecha: 2026-06-09
--
-- Cambios:
--   - Nueva tabla `inmobiliaria`.
--   - Nuevas columnas en `agente`: inmobiliaria_id, rol,
--     debe_cambiar_password, activo.
--   - Backfill: cada agente existente queda como DUENO de su
--     propia inmobiliaria recién creada. Funciona con N agentes.
--
-- NO se toca ninguna otra tabla (leads, interaccion, propiedad,
-- operacion, busqueda, evento_operacion, foto_propiedad).
--
-- IMPORTANTE:
--   * Ejecutar este script ANTES de levantar el backend con los
--     cambios. Aunque dev usa ddl-auto=update (Hibernate crearía
--     las columnas solo), el backfill de inmobiliarias y roles lo
--     hace ÚNICAMENTE este script.
--   * En MySQL cada DDL hace commit implícito: la transacción no
--     cubre los ALTER/CREATE como en Postgres. Si algo falla a
--     mitad de camino, revisar el estado con las queries de
--     verificación del final antes de re-ejecutar.
-- ============================================================

-- 1) Tabla inmobiliaria.
CREATE TABLE inmobiliaria (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    fecha_creacion  DATETIME
);

-- 2) Nuevas columnas en agente, primero NULLables para poder backfillear.
ALTER TABLE agente ADD COLUMN inmobiliaria_id BIGINT NULL;
ALTER TABLE agente ADD COLUMN rol VARCHAR(20) NULL;
ALTER TABLE agente ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agente ADD COLUMN activo BOOLEAN NOT NULL DEFAULT true;

-- 3) Backfill: una inmobiliaria por agente existente, agente como DUENO.
--    MySQL no tiene INSERT ... RETURNING, así que se usa una columna
--    temporal de correlación para mapear cada inmobiliaria a su agente
--    sin depender de nombres únicos. Funciona con N agentes.
ALTER TABLE inmobiliaria ADD COLUMN tmp_agente_id BIGINT NULL;

INSERT INTO inmobiliaria (nombre, fecha_creacion, tmp_agente_id)
SELECT CONCAT('Inmobiliaria de ', COALESCE(a.nombre, ''), ' ', COALESCE(a.apellido, '')),
       NOW(),
       a.id
FROM agente a
WHERE a.inmobiliaria_id IS NULL;

UPDATE agente a
JOIN inmobiliaria i ON i.tmp_agente_id = a.id
SET a.inmobiliaria_id = i.id,
    a.rol = 'DUENO';

ALTER TABLE inmobiliaria DROP COLUMN tmp_agente_id;

-- 4) Con el backfill hecho, endurecer las columnas y agregar la FK.
ALTER TABLE agente MODIFY COLUMN inmobiliaria_id BIGINT NOT NULL;
ALTER TABLE agente MODIFY COLUMN rol VARCHAR(20) NOT NULL;
ALTER TABLE agente
    ADD CONSTRAINT fk_agente_inmobiliaria
    FOREIGN KEY (inmobiliaria_id) REFERENCES inmobiliaria(id);

-- 5) Índice para las queries por tenant (matching, equipo, stats).
CREATE INDEX idx_agente_inmobiliaria ON agente (inmobiliaria_id);

-- ============================================================
-- VERIFICACIÓN:
--
-- Debe dar 0:
--   SELECT count(*) FROM agente WHERE inmobiliaria_id IS NULL;
--
-- Debe dar 0:
--   SELECT count(*) FROM agente WHERE rol IS NULL;
--
-- Control agente <-> inmobiliaria (cada agente con su inmobiliaria y rol DUENO):
--   SELECT a.id, a.nombre, a.apellido, a.email, a.rol, a.activo,
--          i.id AS inmobiliaria_id, i.nombre AS inmobiliaria
--   FROM agente a
--   JOIN inmobiliaria i ON i.id = a.inmobiliaria_id
--   ORDER BY a.id;
--
-- Debe coincidir la cantidad de inmobiliarias con la de agentes pre-migración:
--   SELECT count(*) FROM inmobiliaria;
-- ============================================================
