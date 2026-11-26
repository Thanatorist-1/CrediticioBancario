/* Formulario de portal: datos propios de la solicitud y anexos opcionales en JSON Base64. */
(() => {
  'use strict';
  const form = document.getElementById('form-solicitud-cliente');
  if (!form) return;
  const btn = document.getElementById('enviar-solicitud');
  const mensaje = form.querySelector('[data-portal-error]');
  const input = document.getElementById('archivosSolicitud');
  const info = document.getElementById('archivosSeleccionados');
  const MAX_FILE = 5 * 1024 * 1024;
  const MAX_TOTAL = 20 * 1024 * 1024;
  const TIPOS = {'application/pdf': true, 'image/jpeg': true, 'image/png': true};
  let enviando = false;
  input?.addEventListener('change', () => {
    const files = [...input.files];
    info.textContent = files.length ? `${files.length} archivo(s): ${files.map(f => f.name).join(', ')}` : 'Ningún archivo seleccionado.';
  });
  function validarArchivos(files) {
    if (files.length > 20) throw new Error('Máximo 20 documentos por solicitud.');
    let bytes = 0;
    for (const f of files) {
      if (!(f.type in TIPOS)) throw new Error(`Formato no permitido: ${f.name}. Solo PDF, JPG y PNG.`);
      if (f.size === 0 || f.size > MAX_FILE) throw new Error(`El archivo ${f.name} debe pesar entre 1 byte y 5 MB.`);
      bytes += f.size;
    }
    if (bytes > MAX_TOTAL) throw new Error('Los documentos no pueden superar 20 MB en total.');
  }
  function aBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result).split(',')[1]);
      reader.onerror = () => reject(new Error(`No se pudo leer el archivo ${file.name}.`));
      reader.readAsDataURL(file);
    });
  }
  form.addEventListener('submit', async ev => {
    ev.preventDefault();
    if (enviando || !form.reportValidity()) return;
    enviando = true;
    btn.disabled = true;
    btn.textContent = 'Consultando API y evaluando…';
    mensaje.classList.add('d-none');
    const solicitud = {
      tipoCredito: form.elements.tipoCredito.value,
      montoSolicitado: Number(form.elements.montoSolicitado.value),
      plazo: Number(form.elements.plazo.value),
      motivoCredito: form.elements.motivoCredito.value.trim()
    };
    try {
      const files = input ? [...input.files] : [];
      validarArchivos(files);
      const documentos = await Promise.all(files.map(async file => ({
        nombreArchivo: file.name,
        tipoContenido: file.type,
        contenidoBase64: await aBase64(file)
      })));
      const r = await fetch('/api/v1/portal/solicitudes', {
        method: 'POST', credentials: 'same-origin',
        headers: {'Content-Type':'application/json', 'Accept':'application/json'},
        body: JSON.stringify({solicitud, documentos})
      });
      const result = await r.json().catch(() => ({}));
      if (!r.ok) {
        if (r.status === 401) throw new Error('Tu sesión venció. Vuelve a iniciar sesión.');
        if (r.status === 403) throw new Error('Debes ingresar con una cuenta de cliente.');
        throw new Error(result.error || `No se pudo guardar la solicitud (HTTP ${r.status}).`);
      }
      if (typeof result.id !== 'number') throw new Error('El servidor no confirmó el ID de la solicitud.');
      window.location.assign('/portal-cliente?registrada=' + encodeURIComponent(result.id));
    } catch (ex) {
      mensaje.textContent = ex.message || 'Error al registrar la solicitud.';
      mensaje.classList.remove('d-none');
      mensaje.scrollIntoView({behavior:'smooth', block:'nearest'});
      btn.disabled = false;
      btn.textContent = 'Enviar y evaluar solicitud';
      enviando = false;
    }
  });
})();
