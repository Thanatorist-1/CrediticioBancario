-- SOLO políticas y personal interno. Ningún cliente, solicitud o evaluación ficticios se precarga.
BEGIN;

-- Catálogo de productos: solo crea los predeterminados si aún no existen.
INSERT INTO tipos_credito (codigo,nombre,descripcion,activo,fecha_creacion) SELECT 'Personal','Crédito personal','Producto inicial de demostración',TRUE,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM tipos_credito WHERE codigo='Personal');
INSERT INTO tipos_credito (codigo,nombre,descripcion,activo,fecha_creacion) SELECT 'Vehicular','Crédito vehicular','Producto inicial de demostración',TRUE,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM tipos_credito WHERE codigo='Vehicular');
INSERT INTO tipos_credito (codigo,nombre,descripcion,activo,fecha_creacion) SELECT 'Hipotecario','Crédito hipotecario','Producto inicial de demostración',TRUE,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM tipos_credito WHERE codigo='Hipotecario');
INSERT INTO tipos_credito (codigo,nombre,descripcion,activo,fecha_creacion) SELECT 'Emprendimiento','Crédito para emprendimiento','Producto inicial de demostración',TRUE,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM tipos_credito WHERE codigo='Emprendimiento');
INSERT INTO tipos_credito (codigo,nombre,descripcion,activo,fecha_creacion) SELECT 'Educativo','Crédito educativo','Producto inicial de demostración',TRUE,CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM tipos_credito WHERE codigo='Educativo');

-- Politicas que se mencionan en el campo de politica de cada evaluacion.
INSERT INTO politicas (codigo, producto, version_politica, vigencia, estado, descripcion)
SELECT 'POL-DEMO-001', 'Crédito personal', '1.0', 'Vigente 2026', 'Activa', 'Evaluacion de credito personal: ingresos verificables, historial favorable y capacidad de pago.'
WHERE NOT EXISTS (SELECT 1 FROM politicas WHERE codigo = 'POL-DEMO-001');
INSERT INTO politicas (codigo, producto, version_politica, vigencia, estado, descripcion)
SELECT 'POL-DEMO-002', 'Crédito vehicular', '1.0', 'Vigente 2026', 'Activa', 'Evaluacion de credito vehicular: cuota compatible con el ingreso y documentacion completa.'
WHERE NOT EXISTS (SELECT 1 FROM politicas WHERE codigo = 'POL-DEMO-002');
INSERT INTO politicas (codigo, producto, version_politica, vigencia, estado, descripcion)
SELECT 'POL-DEMO-003', 'Crédito hipotecario', '1.0', 'Vigente 2026', 'Activa', 'Evaluacion hipotecaria: capacidad de pago, estabilidad de ingresos y revision documental.'
WHERE NOT EXISTS (SELECT 1 FROM politicas WHERE codigo = 'POL-DEMO-003');
INSERT INTO politicas (codigo, producto, version_politica, vigencia, estado, descripcion)
SELECT 'POL-DEMO-004', 'Crédito para emprendimiento', '1.0', 'Vigente 2026', 'Activa', 'Criterio de demostración para pequeños negocios: revisar ingresos verificables, flujo de caja, antigüedad de actividad, cuota estimada y documentos de sustento.'
WHERE NOT EXISTS (SELECT 1 FROM politicas WHERE codigo = 'POL-DEMO-004');
INSERT INTO politicas (codigo, producto, version_politica, vigencia, estado, descripcion)
SELECT 'POL-DEMO-005', 'Crédito educativo', '1.0', 'Vigente 2026', 'Activa', 'Criterio de demostración para estudios: validar matrícula o programa, ingresos del solicitante o responsable, cuota estimada y capacidad mensual.'
WHERE NOT EXISTS (SELECT 1 FROM politicas WHERE codigo = 'POL-DEMO-005');

-- Completa límites solo si están vacíos; no reemplaza cambios guardados desde la pantalla Políticas.
UPDATE politicas SET puntaje_minimo_aprobar=COALESCE(puntaje_minimo_aprobar,650),puntaje_maximo_rechazo=COALESCE(puntaje_maximo_rechazo,499),dti_maximo_aprobacion=COALESCE(dti_maximo_aprobacion,35),dti_minimo_rechazo=COALESCE(dti_minimo_rechazo,50) WHERE codigo='POL-DEMO-001';
UPDATE politicas SET puntaje_minimo_aprobar=COALESCE(puntaje_minimo_aprobar,630),puntaje_maximo_rechazo=COALESCE(puntaje_maximo_rechazo,479),dti_maximo_aprobacion=COALESCE(dti_maximo_aprobacion,40),dti_minimo_rechazo=COALESCE(dti_minimo_rechazo,55) WHERE codigo='POL-DEMO-002';
UPDATE politicas SET puntaje_minimo_aprobar=COALESCE(puntaje_minimo_aprobar,680),puntaje_maximo_rechazo=COALESCE(puntaje_maximo_rechazo,519),dti_maximo_aprobacion=COALESCE(dti_maximo_aprobacion,30),dti_minimo_rechazo=COALESCE(dti_minimo_rechazo,45) WHERE codigo='POL-DEMO-003';
UPDATE politicas SET puntaje_minimo_aprobar=COALESCE(puntaje_minimo_aprobar,660),puntaje_maximo_rechazo=COALESCE(puntaje_maximo_rechazo,499),dti_maximo_aprobacion=COALESCE(dti_maximo_aprobacion,35),dti_minimo_rechazo=COALESCE(dti_minimo_rechazo,50) WHERE codigo='POL-DEMO-004';
UPDATE politicas SET puntaje_minimo_aprobar=COALESCE(puntaje_minimo_aprobar,620),puntaje_maximo_rechazo=COALESCE(puntaje_maximo_rechazo,479),dti_maximo_aprobacion=COALESCE(dti_maximo_aprobacion,40),dti_minimo_rechazo=COALESCE(dti_minimo_rechazo,55) WHERE codigo='POL-DEMO-005';

-- Personal interno de ejemplo. Si no tienen clave protegida, el inicio de la app asigna una clave de demostración.
INSERT INTO usuarios (nombre_completo, correo, cargo, rol, estado)
SELECT 'Andrea Salazar', 'andrea.salazar@banco.com', 'Analista de créditos', 'Analista', 'Activo'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = 'andrea.salazar@banco.com');
INSERT INTO usuarios (nombre_completo, correo, cargo, rol, estado)
SELECT 'Roberto Mendoza', 'roberto.mendoza@banco.com', 'Supervisor de créditos', 'Supervisor', 'Activo'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = 'roberto.mendoza@banco.com');


COMMIT;
