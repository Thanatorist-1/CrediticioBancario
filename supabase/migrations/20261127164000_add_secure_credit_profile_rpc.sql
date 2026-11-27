-- Migración aplicada al proyecto datos-crediticios-simulados el 2026-11-27.
-- Mantiene los perfiles privados en app y expone solo una función RPC para el servidor.
alter table app.perfiles_crediticios
  add column if not exists trabaja_actualmente boolean,
  add column if not exists saldo_deuda_pendiente numeric(14,2);

update app.perfiles_crediticios
set trabaja_actualmente = ingresos_mensuales > 0
where trabaja_actualmente is null;

update app.perfiles_crediticios
set saldo_deuda_pendiente = round(coalesce(deuda_mensual, 0) * 12, 2)
where saldo_deuda_pendiente is null;

alter table app.perfiles_crediticios
  alter column trabaja_actualmente set not null,
  alter column saldo_deuda_pendiente set not null;

grant usage on schema app to service_role;
grant select (dni, nombres, apellidos, fecha_nacimiento, identidad_verificada, activo,
              score_crediticio, clasificacion_riesgo, ingresos_mensuales, deuda_mensual,
              cuotas_atrasadas, historial_negativo, trabaja_actualmente, saldo_deuda_pendiente)
  on table app.perfiles_crediticios to service_role;

create or replace function public.obtener_perfil_crediticio(p_dni text)
returns table (
  entorno text, fuente text, dni text, nombre_completo text, nombres text, apellidos text,
  fecha_nacimiento date, identidad_verificada boolean, persona_activa boolean,
  trabaja_actualmente boolean, score_crediticio integer, clasificacion_riesgo text,
  ingresos_mensuales numeric, deuda_mensual numeric, saldo_deuda_pendiente numeric,
  cuotas_atrasadas integer, historial_negativo boolean, advertencia text
)
language sql security invoker set search_path = '' as $$
  select 'SIMULADO'::text, 'Supabase · datos académicos simulados'::text, p.dni,
    concat_ws(' ', p.nombres, p.apellidos), p.nombres, p.apellidos, p.fecha_nacimiento,
    p.identidad_verificada, p.activo, p.trabaja_actualmente, p.score_crediticio,
    p.clasificacion_riesgo, p.ingresos_mensuales, p.deuda_mensual,
    p.saldo_deuda_pendiente, p.cuotas_atrasadas, p.historial_negativo,
    'Datos exclusivamente simulados para fines académicos.'::text
  from app.perfiles_crediticios p where p.dni = p_dni
$$;

revoke all on function public.obtener_perfil_crediticio(text) from public, anon, authenticated;
grant execute on function public.obtener_perfil_crediticio(text) to service_role;
notify pgrst, 'reload schema';
