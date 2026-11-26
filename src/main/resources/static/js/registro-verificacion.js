(() => {
  const dni = document.getElementById('dni');
  const estado = document.getElementById('registro-verificacion-estado');
  const boton = document.getElementById('botonCrearCuenta');
  const verificar = document.getElementById('verificarDniRegistro');
  let validado = null;
  if (!dni || !verificar) return;
  const desactivar = () => {
    validado = null; boton.disabled = true;
    const empleo = document.getElementById('trabajaActualmenteInfo'); if (empleo) empleo.value = '';
    ['nombre','fechaNacimiento','ingresosMensuales','deudasMensuales'].forEach(id => {
      const el = document.getElementById(id); if (el) el.value = '';
    });
  };
  dni.addEventListener('input', () => { desactivar(); estado.textContent='Verifica el DNI ficticio para habilitar el registro.'; });
  verificar.addEventListener('click', async () => {
    desactivar();
    if (!/^9[0-9]{7}$/.test(dni.value)) {estado.textContent='Usa un DNI de prueba que empiece por 9 y tenga 8 dígitos.'; return;}
    verificar.disabled=true; estado.textContent='Consultando API ficticia...';
    try {
      const r = await fetch(`/api/v1/registro/verificar/${encodeURIComponent(dni.value)}`, {headers:{'Accept':'application/json'}});
      const p = await r.json();
      if (!r.ok) throw new Error(p.error || 'No se encuentra o no está activo en la API.');
      document.getElementById('nombre').value = p.nombreCompleto;
      document.getElementById('fechaNacimiento').value = p.fechaNacimiento;
      document.getElementById('ingresosMensuales').value = p.ingresosMensuales;
      document.getElementById('deudasMensuales').value = p.deudaMensual;
      const campoTrabajo = document.getElementById('trabajaActualmenteInfo');
      if (campoTrabajo) campoTrabajo.value = p.trabajaActualmente ? 'Sí, trabaja actualmente' : 'No trabaja actualmente';
      if (!p.trabajaActualmente) {
        const empresa = document.getElementById('empresa'), cargo = document.getElementById('cargo');
        const antiguedad = document.getElementById('antiguedadMeses');
        if (empresa) empresa.value = 'Sin empleo actual';
        if (cargo) cargo.value = 'No aplica';
        if (antiguedad) antiguedad.value = 0;
      }
      validado=p.dni; boton.disabled=false;
      estado.textContent='Perfil ficticio verificado. Completa los demás campos y crea tu cuenta. Se comprobará nuevamente en el servidor.';
    } catch (e) { estado.textContent=e.message; }
    finally { verificar.disabled=false; }
  });
  dni.closest('form')?.addEventListener('submit', e => {
    if (validado !== dni.value) {e.preventDefault(); estado.textContent='Primero verifica el DNI con la API.';}
  }, true);
})();
