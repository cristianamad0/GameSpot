// CACS-27: interfaz de la consumidora. Todas las llamadas van con Fetch al backend de la
// consumidora (/api/portal/...), que a su vez llama a los proveedores con RestClient.

const ZONA = 'America/Bogota';
const CLAVE_SESION = 'gamespot.soa.sesion';

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fechaHora = new Intl.DateTimeFormat('es-CO', { timeZone: ZONA, dateStyle: 'medium', timeStyle: 'short' });
const hora = new Intl.DateTimeFormat('es-CO', { timeZone: ZONA, hour: 'numeric', minute: '2-digit' });
const ETIQUETAS = { PENDIENTE_PAGO: 'Pendiente de pago', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada' };
const MOTIVOS = { OCUPADO: 'ocupado', PASADO: 'ya pasó', FUERA_DE_SERVICIO: 'fuera de servicio' };

const $ = (id) => document.getElementById(id);

// ---------- Sesión (el JWT queda en sessionStorage del navegador) ----------
function sesion() {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE_SESION));
  } catch {
    return null;
  }
}

function guardarSesion(token) {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify(token));
}

function cerrarSesion() {
  sessionStorage.removeItem(CLAVE_SESION);
  pintarSesion();
  cargarEquipos();
  cargarMisReservas();
}

// ---------- Fetch con manejo de errores ----------
async function api(ruta, opciones = {}) {
  const encabezados = { ...(opciones.headers || {}) };
  if (opciones.body !== undefined) {
    encabezados['Content-Type'] = 'application/json';
  }
  const actual = sesion();
  if (actual && opciones.conToken) {
    encabezados.Authorization = 'Bearer ' + actual.token;
  }
  let respuesta;
  try {
    respuesta = await fetch(ruta, {
      method: opciones.method || 'GET',
      headers: encabezados,
      body: opciones.body !== undefined ? JSON.stringify(opciones.body) : undefined,
    });
  } catch {
    throw new Error('No se pudo conectar con la aplicación consumidora.');
  }
  const texto = await respuesta.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!respuesta.ok) {
    const detalles = datos && datos.detalles && datos.detalles.length ? ' (' + datos.detalles.join('; ') + ')' : '';
    const error = new Error(((datos && datos.mensaje) || 'Error ' + respuesta.status) + detalles);
    error.status = respuesta.status;
    throw error;
  }
  return datos;
}

function mostrarAviso(mensaje, esExito = false) {
  const aviso = $('aviso');
  aviso.textContent = mensaje;
  aviso.className = esExito ? 'aviso exito' : 'aviso';
  aviso.hidden = false;
  aviso.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function ocultarAviso() {
  $('aviso').hidden = true;
}

// ---------- Estado de los proveedores ----------
async function revisarEstado() {
  try {
    const estado = await api('/api/portal/estado');
    pintarEstado('estado-catalogo', estado.catalogo);
    pintarEstado('estado-reservas', estado.reservas);
  } catch (error) {
    mostrarAviso(error.message);
  }
}

function pintarEstado(id, valor) {
  const elemento = $(id);
  const disponible = valor === 'DISPONIBLE';
  elemento.textContent = disponible ? 'disponible' : 'no disponible';
  elemento.className = disponible ? 'ok' : 'caido';
}

// ---------- Login ----------
function pintarSesion() {
  const actual = sesion();
  $('panel-login').hidden = Boolean(actual);
  $('boton-salir').hidden = !actual;
  $('sesion-nombre').textContent = actual ? actual.nombre + (actual.rol === 'ADMIN' ? ' (administrador)' : '') : '';
}

$('form-login').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  ocultarAviso();
  try {
    const token = await api('/api/portal/login', {
      method: 'POST',
      body: { email: $('login-email').value.trim(), password: $('login-password').value },
    });
    guardarSesion(token);
    $('login-password').value = '';
    pintarSesion();
    mostrarAviso('Bienvenido, ' + token.nombre + '.', true);
    cargarEquipos();
    cargarMisReservas();
  } catch (error) {
    mostrarAviso(error.message);
  }
});

$('boton-salir').addEventListener('click', cerrarSesion);

// ---------- Equipos (proveedor de catálogo) ----------
let equipos = [];

async function cargarEquipos() {
  const tabla = $('tabla-equipos');
  try {
    equipos = await api('/api/portal/equipos');
  } catch (error) {
    tabla.innerHTML = '';
    tabla.append(filaMensaje(error.message, 5));
    return;
  }
  const esAdmin = sesion() && sesion().rol === 'ADMIN';
  tabla.innerHTML = '';
  for (const equipo of equipos) {
    const fila = document.createElement('tr');
    fila.append(
      celda(equipo.nombre),
      celda(equipo.modalidad),
      celda(pesos.format(equipo.tarifaHoraPesos)),
      celda(equipo.sePuedeReservar ? 'Disponible' : 'Fuera de servicio'),
    );
    const acciones = document.createElement('td');
    if (esAdmin) {
      const boton = document.createElement('button');
      boton.type = 'button';
      boton.className = equipo.estaDisponible ? 'peligro' : 'secundario';
      boton.textContent = equipo.estaDisponible ? 'Poner fuera de servicio' : 'Habilitar';
      boton.addEventListener('click', () => cambiarEstadoEquipo(equipo));
      acciones.append(boton);
    }
    fila.append(acciones);
    tabla.append(fila);
  }
  const selector = $('disp-equipo');
  const elegido = selector.value;
  selector.innerHTML = '';
  for (const equipo of equipos) {
    const opcion = new Option(equipo.nombre + ' · ' + pesos.format(equipo.tarifaHoraPesos) + '/h', equipo.id);
    selector.append(opcion);
  }
  if (elegido) {
    selector.value = elegido;
  }
}

async function cambiarEstadoEquipo(equipo) {
  ocultarAviso();
  try {
    const actualizado = await api('/api/portal/equipos/' + equipo.id + '/estado', {
      method: 'PATCH',
      body: { estaDisponible: !equipo.estaDisponible },
      conToken: true,
    });
    mostrarAviso(actualizado.nombre + (actualizado.estaDisponible ? ' quedó disponible.' : ' quedó fuera de servicio.'), true);
    cargarEquipos();
  } catch (error) {
    mostrarAviso(error.message);
  }
}

// ---------- Disponibilidad y reserva (operaciones integradas) ----------
function hoyEnBogota() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA }).format(new Date());
}

$('form-disponibilidad').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  await cargarDisponibilidad();
});

async function cargarDisponibilidad() {
  ocultarAviso();
  const contenedor = $('bloques');
  contenedor.innerHTML = '';
  const equipoId = $('disp-equipo').value;
  const fecha = $('disp-fecha').value;
  if (!equipoId || !fecha) {
    mostrarAviso('Elige un equipo y una fecha.');
    return;
  }
  try {
    const disponibilidad = await api('/api/portal/equipos/' + equipoId + '/disponibilidad?fecha=' + fecha);
    if (!disponibilidad.atiende) {
      contenedor.textContent = 'El local no atiende ese día.';
      return;
    }
    for (const bloque of disponibilidad.bloques) {
      const boton = document.createElement('button');
      boton.type = 'button';
      boton.className = 'bloque ' + (bloque.estaLibre ? 'libre' : 'no-libre');
      boton.disabled = !bloque.estaLibre;
      boton.textContent = hora.format(new Date(bloque.inicio)) + (bloque.estaLibre ? ' · libre' : ' · ' + MOTIVOS[bloque.motivo]);
      boton.addEventListener('click', () => {
        document.querySelectorAll('.bloque.elegido').forEach((b) => b.classList.remove('elegido'));
        boton.classList.add('elegido');
        $('res-hora').value = new Intl.DateTimeFormat('en-GB', { timeZone: ZONA, hour: '2-digit', minute: '2-digit' }).format(new Date(bloque.inicio));
      });
      contenedor.append(boton);
    }
  } catch (error) {
    mostrarAviso(error.message);
  }
}

$('form-reserva').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  ocultarAviso();
  if (!sesion()) {
    mostrarAviso('Inicia sesión para reservar.');
    return;
  }
  try {
    const reserva = await api('/api/portal/reservas', {
      method: 'POST',
      conToken: true,
      body: {
        equipoId: Number($('disp-equipo').value),
        fecha: $('disp-fecha').value || null,
        hora: $('res-hora').value || null,
        duracionMinutos: Number($('res-duracion').value),
      },
    });
    mostrarAviso(
      'Reserva creada: ' + reserva.nombreEquipo + ', ' + fechaHora.format(new Date(reserva.inicio)) +
        '. Paga el adelanto de ' + pesos.format(reserva.adelantoPesos) + ' para confirmarla.',
      true,
    );
    cargarDisponibilidad();
    cargarMisReservas();
  } catch (error) {
    mostrarAviso(error.message);
    if (error.status === 401) {
      cerrarSesion();
    }
  }
});

// ---------- Mis reservas (proveedor de reservas) ----------
async function cargarMisReservas() {
  const tabla = $('tabla-reservas');
  tabla.innerHTML = '';
  if (!sesion()) {
    tabla.append(filaMensaje('Inicia sesión para ver tus reservas.', 6));
    return;
  }
  let reservas;
  try {
    reservas = await api('/api/portal/reservas/mias', { conToken: true });
  } catch (error) {
    tabla.append(filaMensaje(error.message, 6));
    return;
  }
  if (reservas.length === 0) {
    tabla.append(filaMensaje('Aún no tienes reservas.', 6));
    return;
  }
  for (const reserva of reservas) {
    const fila = document.createElement('tr');
    const estado = document.createElement('span');
    estado.className = 'etiqueta ' + reserva.estado;
    estado.textContent = ETIQUETAS[reserva.estado] || reserva.estado;
    const celdaEstado = document.createElement('td');
    celdaEstado.append(estado);
    fila.append(
      celda(reserva.nombreEquipo),
      celda(fechaHora.format(new Date(reserva.inicio)) + ' – ' + hora.format(new Date(reserva.fin))),
      celda(pesos.format(reserva.precioTotalPesos)),
      celda(pesos.format(reserva.adelantoPesos)),
      celdaEstado,
    );
    const acciones = document.createElement('td');
    if (reserva.estado === 'PENDIENTE_PAGO') {
      acciones.append(botonAccion('Pagar adelanto', '', () => cambiarEstadoReserva(reserva.id, 'pago')));
    }
    if (reserva.estado !== 'CANCELADA') {
      acciones.append(botonAccion('Cancelar', 'peligro', () => cambiarEstadoReserva(reserva.id, 'cancelacion')));
    }
    fila.append(acciones);
    tabla.append(fila);
  }
}

async function cambiarEstadoReserva(id, accion) {
  ocultarAviso();
  try {
    const reserva = await api('/api/portal/reservas/' + id + '/' + accion, { method: 'PATCH', conToken: true });
    mostrarAviso('Reserva ' + (accion === 'pago' ? 'confirmada' : 'cancelada') + ': ' + reserva.nombreEquipo + '.', true);
    cargarMisReservas();
    if ($('bloques').children.length) {
      cargarDisponibilidad();
    }
  } catch (error) {
    mostrarAviso(error.message);
  }
}

// ---------- Ayudas de tabla ----------
function celda(texto) {
  const td = document.createElement('td');
  td.textContent = texto;
  return td;
}

function filaMensaje(texto, columnas) {
  const fila = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = columnas;
  td.className = 'suave';
  td.textContent = texto;
  fila.append(td);
  return fila;
}

function botonAccion(texto, clase, accion) {
  const boton = document.createElement('button');
  boton.type = 'button';
  boton.className = clase;
  boton.textContent = texto;
  boton.style.marginRight = '6px';
  boton.addEventListener('click', accion);
  return boton;
}

// ---------- Inicio ----------
$('boton-estado').addEventListener('click', revisarEstado);
$('disp-fecha').value = hoyEnBogota();
pintarSesion();
revisarEstado();
cargarEquipos();
cargarMisReservas();
