-- El portal usa JDBC desde el servidor; el Data API público no debe leer estas tablas.
alter table public.auditoria_configuracion enable row level security;
alter table public.clientes enable row level security;
alter table public.documentos_solicitud enable row level security;
alter table public.evaluaciones enable row level security;
alter table public.politicas enable row level security;
alter table public.solicitudes enable row level security;
alter table public.tipos_credito enable row level security;
alter table public.usuarios enable row level security;
