/* Conexión local entre Sistema Crediticio (8080) y API simulada (8081). */
(() => {
  'use strict';
  const campos = [
    ['dni', 'DNI ficticio'], ['nombreCompleto', 'Nombre completo'],
    ['nombres', 'Nombres'], ['apellidos', 'Apellidos'],
    ['identidadVerificada', 'Identidad verificada'], ['personaActiva', 'Persona activa'],
    ['trabajaActualmente', 'Trabaja actualmente'],
    ['scoreCrediticio', 'Puntaje crediticio'], ['clasificacionRiesgo', 'Nivel de riesgo'],
    ['ingresosMensuales', 'Ingresos mensuales (S/)'], ['deudaMensual', 'Cuota de deuda mensual (S/)'],
    ['saldoDeudaPendiente', 'Saldo total pendiente (S/)'],
    ['cuotasAtrasadas', 'Cuotas atrasadas'], ['historialNegativo', 'Historial negativo'],
    ['fechaNacimiento', 'Fecha de nacimiento'], ['fuente', 'Fuente']
  ];
  const valor = x => x === null || x === undefined ? 'No disponible' : typeof x === 'boolean' ? (x ? 'Sí' : 'No') : String(x);
  function mostrar(parent, datos, info) {
    parent.replaceChildren();
    for (const [key, titulo] of campos) {
      const div = document.createElement('div'); div.className = 'col-sm-6 col-lg-4';
      const etiqueta = document.createElement('div'); etiqueta.className = 'small text-muted'; etiqueta.textContent = titulo;
      const contenido = document.createElement('div'); contenido.className = 'fw-semibold'; contenido.textContent = valor(datos[key]);
      div.append(etiqueta, contenido); parent.append(div);
    }
    info.textContent = datos.advertencia || 'Información de demostración; no utilizar para decisiones reales.';
  }
  async function consultar(dni) {
    const respuesta = await fetch('/api/v1/consultas-simuladas/' + encodeURIComponent(dni), {
      method: 'GET', credentials: 'same-origin', headers: {'Accept': 'application/json'}
    });
    const datos = await respuesta.json();
    if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || 'Error HTTP ' + respuesta.status);
    return datos;
  }
  const form = document.getElementById('formConsultaSimulada');
  if (form) {
    const error = document.getElementById('errorConsulta');
    const panel = document.getElementById('resultadoConsulta');
    const grid = document.getElementById('camposConsulta');
    const aviso = document.getElementById('avisoConsulta');
    form.addEventListener('submit', async event => {
      event.preventDefault();
      error.hidden = true; panel.hidden = true;
      if (!form.reportValidity()) return;
      const boton = form.querySelector('button[type="submit"]'); boton.disabled = true;
      try { mostrar(grid, await consultar(form.elements.dni.value.trim()), aviso); panel.hidden = false; }
      catch (e) { error.textContent = e.message; error.hidden = false; }
      finally { boton.disabled = false; }
    });
  }
  const botonDetalle = document.getElementById('consultarDniDetalle');
  if (botonDetalle) {
    const error = document.getElementById('errorSimuladoDetalle');
    const panel = document.getElementById('panelSimuladoDetalle');
    const grid = document.getElementById('camposSimuladoDetalle');
    const aviso = document.getElementById('avisoSimuladoDetalle');
    botonDetalle.addEventListener('click', async () => {
      error.hidden = true; panel.hidden = true;
      const dni = botonDetalle.dataset.dni || '';
      if (!/^9[0-9]{7}$/.test(dni)) {
        error.textContent = 'El DNI ' + dni + ' no corresponde a los perfiles sintéticos. Usa una solicitud de demostración con 90000001.';
        error.hidden = false; return;
      }
      botonDetalle.disabled = true;
      try { mostrar(grid, await consultar(dni), aviso); panel.hidden = false; }
      catch (e) { error.textContent = e.message; error.hidden = false; }
      finally { botonDetalle.disabled = false; }
    });
  }
  const botonEvaluacion = document.getElementById('consultarDniSolicitud');
  if (botonEvaluacion) {
    const select = document.getElementById('idSolicitud');
    const error = document.getElementById('errorSimuladoEvaluacion');
    const panel = document.getElementById('panelSimuladoEvaluacion');
    const grid = document.getElementById('camposSimuladoEvaluacion');
    const aviso = document.getElementById('avisoSimuladoEvaluacion');
    const comentario = document.getElementById('notaSimuladoEvaluacion');
    select?.addEventListener('change', () => { error.hidden = true; panel.hidden = true; });
    botonEvaluacion.addEventListener('click', async () => {
      error.hidden = true; panel.hidden = true; comentario.textContent = '';
      const opcion = select?.selectedOptions?.[0];
      const dni = opcion?.dataset?.dni;
      if (!dni) { error.textContent = 'Selecciona primero una solicitud vinculada a un cliente.'; error.hidden = false; return; }
      if (!/^9[0-9]{7}$/.test(dni)) {
        error.textContent = 'El cliente tiene el DNI ' + dni + '. La API de demostración solo admite DNI ficticios que empiecen por 9. Registra una solicitud de prueba con 90000001.';
        error.hidden = false; return;
      }
      botonEvaluacion.disabled = true;
      try {
        const datos = await consultar(dni);
        mostrar(grid, datos, aviso); panel.hidden = false;
        // Solo propone valores; la persona responsable revisa y guarda la evaluación.
        const puntaje = document.getElementById('puntaje');
        const riesgo = document.getElementById('nivelRiesgo');
        const dti = document.getElementById('dti');
        const riesgoValido = ['Bajo', 'Medio', 'Alto'].includes(datos.clasificacionRiesgo);
        if (puntaje) puntaje.value = datos.identidadVerificada && Number.isInteger(datos.scoreCrediticio) ? String(datos.scoreCrediticio) : '';
        if (riesgo) riesgo.value = riesgoValido ? datos.clasificacionRiesgo : 'Por evaluar';
        const ingresos = Number(datos.ingresosMensuales), deuda = Number(datos.deudaMensual);
        if (dti && ingresos > 0 && Number.isFinite(deuda) && deuda >= 0) {
          const ratio = deuda / ingresos * 100;
          dti.value = ratio <= 100 ? ratio.toFixed(2) : '';
        } else if (dti) { dti.value = ''; }
        comentario.textContent = 'Se han sugerido puntaje, riesgo y DTI cuando existen datos suficientes. El DTI es deuda mensual / ingresos mensuales (sin nueva cuota). Revisa antes de guardar; el resultado de aprobación o rechazo NO se modifica automáticamente.';
      } catch (e) { error.textContent = e.message; error.hidden = false; }
      finally { botonEvaluacion.disabled = false; }
    });
  }
})();
