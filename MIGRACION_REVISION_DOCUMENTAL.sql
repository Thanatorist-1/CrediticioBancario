-- Ejecutar UNA SOLA VEZ desde pgAdmin Query Tool sobre la base crediticio_db.
-- No elimina clientes, solicitudes, evaluaciones ni documentos existentes.
BEGIN;
ALTER TABLE public.evaluaciones
    ALTER COLUMN comentario TYPE VARCHAR(1000);
ALTER TABLE public.evaluaciones
    ADD COLUMN IF NOT EXISTS requerimiento_documental VARCHAR(800);
ALTER TABLE public.documentos_solicitud
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(40);
ALTER TABLE public.documentos_solicitud
    ADD COLUMN IF NOT EXISTS etapa VARCHAR(20);
UPDATE public.documentos_solicitud SET categoria = 'OTRO' WHERE categoria IS NULL;
UPDATE public.documentos_solicitud SET etapa = 'SOLICITUD' WHERE etapa IS NULL;
COMMIT;
