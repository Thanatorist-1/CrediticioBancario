/* Inicio de sesión usando JSON enviado desde el frontend al API REST. */
(() => {
    const formulario = document.getElementById('login-json-form');
    if (!formulario) return;
    const error = document.getElementById('login-json-error');
    const boton = formulario.querySelector('button[type="submit"]');
    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        if (!formulario.reportValidity()) return;
        error.hidden = true;
        error.textContent = '';
        boton.disabled = true;
        try {
            const datos = {
                usuario: formulario.elements.usuario.value.trim(),
                password: formulario.elements.password.value
            };
            const respuesta = await fetch('/api/v1/auth/login', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(datos)
            });
            const resultado = await respuesta.json();
            if (!respuesta.ok) {
                throw new Error(resultado.error || 'No se pudo iniciar sesión.');
            }
            window.location.assign(resultado.tipo === 'CLIENTE' ? '/portal-cliente' : '/dashboard');
        } catch (excepcion) {
            error.textContent = excepcion.message || 'Error de conexión con el servidor.';
            error.hidden = false;
        } finally {
            boton.disabled = false;
        }
    });
})();
