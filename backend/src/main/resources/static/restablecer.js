const token = new URLSearchParams(location.hash.slice(1)).get('token');
history.replaceState(null, '', location.pathname);
const form = document.getElementById('reset');
const status = document.getElementById('estado');
if (!token) { form.hidden = true; status.textContent = 'Abre el enlace recibido en tu correo para continuar.'; }
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const password = document.getElementById('password').value;
  if (password !== document.getElementById('confirm').value) { status.textContent = 'Las contraseñas deben coincidir.'; return; }
  if ([...password].length < 12 || new TextEncoder().encode(password).length > 72) { status.textContent = 'Usa al menos 12 caracteres y un máximo de 72 bytes UTF-8.'; return; }
  const button = form.querySelector('button'); button.disabled = true;
  status.textContent = 'Guardando…';
  try {
    const response = await fetch('/api/v1/auth/reset-password', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({token, nuevaPassword: password})});
    if (response.ok) { form.reset(); form.hidden = true; status.textContent = 'Contraseña actualizada. Regresa a Santa Cecilia e inicia sesión nuevamente.'; }
    else { const result = await response.json(); status.textContent = result.message || 'No pudimos restablecer tu acceso.'; }
  } catch { status.textContent = 'No hay conexión. Vuelve a intentarlo.'; }
  finally { button.disabled = false; }
});
