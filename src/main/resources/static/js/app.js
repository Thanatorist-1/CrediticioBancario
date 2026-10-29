// El login es una demostración de navegación, no una autenticación real.
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('[data-form="login"]');
    if (!form) return;

    const user = document.getElementById('usuario');
    form.addEventListener('submit', event => {
        event.preventDefault();
        const value = user.value.trim();
        const worker = /^[^\s@]+@banco\.com$/i.test(value);
        const client = /^\d{8}$/.test(value);

        user.setCustomValidity(worker || client ? '' : 'Ingresa un correo @banco.com o un DNI de 8 dígitos.');
        form.classList.add('was-validated');
        if (!form.checkValidity()) return;

        if (worker) {
            window.location.href = '/dashboard';
        } else {
            window.location.href = '/portal-cliente?dni=' + encodeURIComponent(value);
        }
    });
});
