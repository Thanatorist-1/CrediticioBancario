-- ATENCIÓN: elimina todos los clientes, solicitudes, evaluaciones y documentos
-- incluida la semilla de 50 clientes si se ejecuta después del arranque.
-- Haz BACKUP antes y ejecuta una sola vez, si lo necesitas, SOLO en crediticio_db.
-- Se recomienda ejecutar antes de iniciar el sistema bancario de la nueva versión.
BEGIN;
TRUNCATE TABLE public.documentos_solicitud, public.evaluaciones,
               public.solicitudes, public.clientes RESTART IDENTITY;
COMMIT;
SELECT (SELECT count(*) FROM public.clientes) AS clientes,
       (SELECT count(*) FROM public.solicitudes) AS solicitudes,
       (SELECT count(*) FROM public.evaluaciones) AS evaluaciones;
