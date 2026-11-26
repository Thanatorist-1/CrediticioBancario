(() => {
  'use strict';
  const form = document.querySelector('#form-documentos-revision');
  if (!form) return;
  const input = document.querySelector('#archivosRevision');
  const categoria = document.querySelector('#categoriaRevision');
  const boton = document.querySelector('#boton-documentos-revision');
  const alerta = document.querySelector('#error-documentos-revision');
  const limiteArchivo = 5 * 1024 * 1024;
  const limiteTotal = 20 * 1024 * 1024;
  function error(mensaje) { alerta.textContent = mensaje; alerta.classList.remove('d-none'); alerta.scrollIntoView({block:'nearest'}); }
  function base64(archivo) { return new Promise((resolve,reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(',')[1]);
    reader.onerror = () => reject(new Error('No se pudo leer ' + archivo.name));
    reader.readAsDataURL(archivo);
  }); }
  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!form.reportValidity()) return;
    alerta.classList.add('d-none');
    boton.disabled = true;
    boton.textContent = 'Enviando documentos…';
    try {
      const archivos = [...input.files];
      if (!archivos.length) throw new Error('Selecciona al menos un archivo.');
      if (archivos.length > 20) throw new Error('No se permiten más de 20 documentos.');
      let total = 0;
      const permitidos = ['application/pdf','image/png','image/jpeg'];
      archivos.forEach(f => {
        if (!permitidos.includes(f.type)) throw new Error('Solo se permiten archivos PDF, JPG o PNG.');
        if (f.size <= 0 || f.size > limiteArchivo) throw new Error('Cada archivo debe tener entre 1 byte y 5 MB.');
        total += f.size;
      });
      if (total > limiteTotal) throw new Error('La selección supera los 20 MB permitidos.');
      const documentos = await Promise.all(archivos.map(async f => ({
        nombreArchivo: f.name, tipoContenido: f.type,
        categoria: categoria.value, contenidoBase64: await base64(f)
      })));
      const id = form.dataset.idSolicitud;
      const response = await fetch(`/api/v1/solicitudes/${encodeURIComponent(id)}/documentos`, {
        method:'POST', credentials:'same-origin',
        headers:{'Content-Type':'application/json','Accept':'application/json'},
        body:JSON.stringify({documentos})
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || `Error al guardar documentos (${response.status}).`);
      window.location.reload();
    } catch(ex) { error(ex.message || 'No fue posible enviar los documentos.'); }
    finally { boton.disabled = false; boton.textContent = 'Enviar documentos para revisión'; }
  });
})();
