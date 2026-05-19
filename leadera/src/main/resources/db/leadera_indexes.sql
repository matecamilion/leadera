-- ============================================================
-- Índices de performance para la tabla `leads`.
-- El proyecto no usa Flyway/Liquibase: este script debe ejecutarse
-- manualmente contra la base (MySQL en dev, PostgreSQL en prod).
-- Es idempotente: se puede correr varias veces sin error.
-- ============================================================

-- Índice compuesto principal: cubre la mayoría de queries del dashboard y cartera.
-- Filtra por agente y ordena/filtra por estado.
CREATE INDEX IF NOT EXISTS idx_leads_agente_estado
    ON leads (agente_id, estado);

-- Índice para queries de seguimiento pendiente y leads inactivos.
-- Cubre: findSeguimientosPendientes, obtenerLeadsInactivos.
CREATE INDEX IF NOT EXISTS idx_leads_agente_ultimo_contacto
    ON leads (agente_id, ultimo_contacto);

-- Índice para el home diario: leads con seguimiento programado.
CREATE INDEX IF NOT EXISTS idx_leads_proximo_seguimiento
    ON leads (agente_id, fecha_proximo_seguimiento);

-- Índice para el conteo de ingresos del mes.
CREATE INDEX IF NOT EXISTS idx_leads_fecha_entrada
    ON leads (agente_id, fecha_entrada);
