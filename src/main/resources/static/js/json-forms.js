/** Formularios REST de la interfaz: JSON para altas, ediciones, bajas y decisiones. */
(() => {
  'use strict';
  const MAX_FILE = 5 * 1024 * 1024;
  const MAX_TOTAL = 20 * 1024 * 1024;
  const validMimes = new Set(['application/pdf', 'image/jpeg', 'image/png']);

  function datosFormulario(form, submitter) {
    const data = new FormData(form);
    if (submitter && submitter.name) data.set(submitter.name, submitter.value);
    const result = {};
    for (const [clave, value] of data.entries()) {
      if (value instanceof File) continue;
      const control = form.elements.namedItem(clave);
      const tipoNumerico = control && control.type === 'number';
      result[clave] = tipoNumerico ? (value === '' ? null : Number(value)) : (value === '' ? null : value);
    }
    // Un checkbox desmarcado no aparece en FormData. Enviar false explícito permite
    // desactivar reglas de empleo/garantía y tipos de crédito por JSON.
    for (const control of form.querySelectorAll('input[type="checkbox"][name]')) {
      result[control.name] = control.checked;
    }
    return result;
  }

  function base64(archivo) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result).split(',')[1]);
      reader.onerror = () => reject(new Error(`No se pudo leer: ${archivo.name}`));
      reader.readAsDataURL(archivo);
    });
  }
  async function documentosJSON(form) {
    const input = form.querySelector('input[type="file"][name="archivos"]');
    const archivos = [...(input?.files || [])];
    if (archivos.some(a => a.size > MAX_FILE)) throw new Error('Cada archivo puede pesar como máximo 5 MB.');
    if (archivos.reduce((n, a) => n + a.size, 0) > MAX_TOTAL) throw new Error('El total de adjuntos no debe superar 20 MB.');
    if (archivos.some(a => !validMimes.has(a.type))) throw new Error('Solo se aceptan PDF, JPG o PNG.');
    return Promise.all(archivos.map(async archivo => ({
      nombreArchivo: archivo.name,
      tipoContenido: archivo.type,
      contenidoBase64: await base64(archivo)
    })));
  }

  function mostrarError(form, mensaje) {
    let alerta = form.querySelector('[data-json-error]');
    if (!alerta) {
      alerta = document.createElement('div');
      alerta.className = 'alert alert-danger';
      alerta.setAttribute('role', 'alert');
      alerta.setAttribute('data-json-error', '');
      form.prepend(alerta);
    }
    alerta.textContent = mensaje;
    alerta.hidden = false;
    alerta.scrollIntoView({block: 'nearest', behavior: 'smooth'});
  }
  function ocultarError(form) {
    const alerta = form.querySelector('[data-json-error]');
    if (alerta) alerta.hidden = true;
  }

  async function procesar(form, submitter) {
    if (!form.reportValidity()) return;
    if (form.dataset.jsonConfirm && !window.confirm(form.dataset.jsonConfirm)) return;
    const accion = submitter || form.querySelector('[type="submit"]');
    if (accion) accion.disabled = true;
    ocultarError(form);
    try {
      const datos = datosFormulario(form, submitter);
      const id = form.dataset.jsonIdField && datos[form.dataset.jsonIdField];
      const metodo = form.dataset.jsonMethod || (id ? 'PUT' : 'POST');
      let ruta = form.dataset.jsonApi;
      if (id && !form.dataset.jsonMethod) ruta += `/${encodeURIComponent(id)}`;
      let body = datos;
      if (form.dataset.jsonDocuments === 'true') {
        const documentos = await documentosJSON(form);
        body = {solicitud: datos, documentos};
      }
      const respuesta = await fetch(ruta, {
        method: metodo,
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        body: JSON.stringify(metodo === 'DELETE' ? {} : body)
      });
      const tipo = respuesta.headers.get('content-type') || '';
      const payload = tipo.includes('application/json') ? await respuesta.json() : null;
      if (!respuesta.ok) throw new Error(payload?.error || `Error HTTP ${respuesta.status}`);
      window.location.assign(form.dataset.jsonRedirect || window.location.href);
    } catch (error) {
      mostrarError(form, error.message || 'No fue posible completar la operación.');
    } finally {
      if (accion) accion.disabled = false;
    }
  }

  document.addEventListener('submit', (evento) => {
    const form = evento.target;
    if (!(form instanceof HTMLFormElement) || !form.dataset.jsonApi) return;
    evento.preventDefault();
    void procesar(form, evento.submitter);
  });

  // El cierre de sesión también se envía con un POST JSON.
  document.addEventListener('click', async (evento) => {
    const enlace = evento.target.closest('a[href]');
    if (!enlace || new URL(enlace.href).pathname !== '/logout') return;
    evento.preventDefault();
    try {
      await fetch('/api/v1/auth/logout', {
        method: 'POST', credentials: 'same-origin',
        headers: {'Content-Type':'application/json', 'Accept':'application/json'},
        body: JSON.stringify({})
      });
      window.location.assign('/login');
    } catch { window.location.assign('/login'); }
  });
})();
