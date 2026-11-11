// CACS-27 y CACS-29: interfaz de la consumidora. Todas las llamadas van con Fetch al backend de
// la consumidora (/api/portal/...), que a su vez llama a los proveedores con RestClient.

const ZONA = 'America/Bogota';
const CLAVE_SESION = 'gamespot.soa.sesion';
const PORCENTAJE_ADELANTO = 50;

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fechaLarga = new Intl.DateTimeFormat('es-CO', { timeZone: ZONA, weekday: 'long', day: 'numeric', month: 'long' });
const fechaHora = new Intl.DateTimeFormat('es-CO', { timeZone: ZONA, dateStyle: 'medium', timeStyle: 'short' });
const hora = new Intl.DateTimeFormat('es-CO', { timeZone: ZONA, hour: 'numeric', minute: '2-digit' });
const hora24 = new Intl.DateTimeFormat('en-GB', { timeZone: ZONA, hour: '2-digit', minute: '2-digit' });
const ETIQUETAS = { PENDIENTE_PAGO: 'Pendiente de pago', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada' };
const MOTIVOS = { OCUPADO: 'Ocupado', PASADO: 'Ya pasó', FUERA_DE_SERVICIO: 'Fuera de servicio' };
const DIAS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
// CACS-31: ilustraciones propias de cada modalidad (img/consola-<modalidad>.svg). Si una modalidad
// nueva no tiene ilustración, se muestra el ícono de control.
const ILUSTRACIONES = ['xbox-360', 'xbox-series-s', 'sala-vip-xbox-series-x'];
// CACS-32: niveles de cada modalidad, como en la publicidad de tarifas del local.
const NIVELES = {
  'xbox-360': 'Nivel 1 · Clásicos',
  'xbox-series-s': 'Nivel 2 · Nueva Gen',
  'sala-vip-xbox-series-x': 'Nivel 3 · Élite',
};
const DESCRIPCIONES = {
  'xbox-360': 'Los clásicos de siempre para jugar con amigos.',
  'xbox-series-s': 'Nueva generación, juegos rápidos y en alta resolución.',
  'sala-vip-xbox-series-x': 'Sala privada con Series X, ideal para grupos y torneos.',
};

// Fotos del local en static/img/. Cuando tengan una foto, cópienla a esa carpeta y pongan aquí el
// nombre del archivo; mientras sea null se muestra la ilustración de la modalidad.
const FOTOS = {
  local: null, // foto de fondo de la portada, por ejemplo 'local.jpg'
  'xbox-360': null,
  'xbox-series-s': null,
  'sala-vip-xbox-series-x': null,
};

const $ = (id) => document.getElementById(id);

// Estado de la reserva que se está armando.
const reserva = { equipoId: null, fecha: null, inicio: null, duracionMinutos: 60, creada: null };
let equipos = [];
// CACS-32: catálogo de juegos de cada equipo (static/datos/juegos.json, tomado de los catálogos
// del local). Es contenido de la página, no viene de los proveedores.
let catalogoJuegos = {};
let pestanaJuegos = null;
// CACS-36: carátula (img/caratulas/) de los juegos que la tienen, por título.
let caratulas = {};
let pasoActual = 1;

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
  if (pasoActual === 3) {
    pintarResumen();
  }
}

// ---------- Fetch con manejo de errores ----------
async function api(ruta, opciones = {}) {
  const encabezados = {};
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
  let datos = null;
  try {
    datos = texto ? JSON.parse(texto) : null;
  } catch {
    datos = null;
  }
  if (!respuesta.ok) {
    const detalles = datos && datos.detalles && datos.detalles.length ? ' (' + datos.detalles.join('; ') + ')' : '';
    const error = new Error(((datos && datos.mensaje) || 'Error ' + respuesta.status) + detalles);
    error.status = respuesta.status;
    throw error;
  }
  return datos;
}

let temporizadorAviso;

function mostrarAviso(mensaje, esExito = false) {
  const aviso = $('aviso');
  aviso.textContent = mensaje;
  aviso.className = esExito ? 'aviso exito' : 'aviso';
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => (aviso.hidden = true), esExito ? 5000 : 8000);
}

function ocultarAviso() {
  $('aviso').hidden = true;
}

function elemento(etiqueta, clase, texto) {
  const nuevo = document.createElement(etiqueta);
  if (clase) {
    nuevo.className = clase;
  }
  if (texto !== undefined) {
    nuevo.textContent = texto;
  }
  return nuevo;
}

function boton(texto, clase, accion) {
  const nuevo = elemento('button', 'boton ' + (clase || ''), texto);
  nuevo.type = 'button';
  nuevo.addEventListener('click', accion);
  return nuevo;
}

function clave(texto) {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
}

// ---------- Estado de los proveedores ----------
async function revisarEstado() {
  try {
    const estado = await api('/api/portal/estado');
    pintarEstado('estado-catalogo', estado.catalogo);
    pintarEstado('estado-reservas', estado.reservas);
    const caidos = [];
    if (estado.catalogo !== 'DISPONIBLE') {
      caidos.push('catálogo (equipos, precios y horario)');
    }
    if (estado.reservas !== 'DISPONIBLE') {
      caidos.push('reservas (ingreso y reservas)');
    }
    if (caidos.length) {
      mostrarAviso('Servicio no disponible: ' + caidos.join(' y ') + '. Lo demás sigue funcionando.');
    }
  } catch (error) {
    mostrarAviso(error.message);
  }
}

function pintarEstado(id, valor) {
  const nodo = $(id);
  const disponible = valor === 'DISPONIBLE';
  nodo.textContent = disponible ? 'disponible' : 'no disponible';
  nodo.className = disponible ? 'ok' : 'caido';
}

// ---------- Ingreso ----------
function pintarSesion() {
  const actual = sesion();
  const esAdmin = Boolean(actual && actual.rol === 'ADMIN');
  $('boton-ingresar').hidden = Boolean(actual);
  $('boton-salir').hidden = !actual;
  $('sesion-nombre').textContent = actual ? 'Hola, ' + actual.nombre + (esAdmin ? ' (administrador)' : '') : '';
  $('mis-reservas').hidden = !actual;
  $('enlace-mis-reservas').hidden = !actual;
  $('admin').hidden = !esAdmin;
  $('enlace-admin').hidden = !esAdmin;
  cargarMisReservas();
}

function abrirLogin() {
  $('error-login').hidden = true;
  $('dialogo-login').showModal();
  $('login-email').focus();
}

$('boton-ingresar').addEventListener('click', abrirLogin);
$('boton-login-confirmar').addEventListener('click', abrirLogin);
$('boton-cerrar-login').addEventListener('click', () => $('dialogo-login').close());
$('boton-salir').addEventListener('click', cerrarSesion);

$('form-login').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  $('error-login').hidden = true;
  try {
    const token = await api('/api/portal/login', {
      method: 'POST',
      body: { email: $('login-email').value.trim(), password: $('login-password').value },
    });
    guardarSesion(token);
    $('login-password').value = '';
    $('dialogo-login').close();
    pintarSesion();
    cargarEquipos();
    if (pasoActual === 3) {
      pintarResumen();
    }
    mostrarAviso('Bienvenido, ' + token.nombre + '.', true);
  } catch (error) {
    $('error-login').textContent = error.message;
    $('error-login').hidden = false;
  }
});

// ---------- Equipos (proveedor de catálogo) ----------
async function cargarEquipos() {
  try {
    equipos = await api('/api/portal/equipos');
  } catch (error) {
    equipos = [];
    const lista = $('lista-modalidades');
    lista.replaceChildren(elemento('li', 'suave', error.message));
    $('opciones-equipo').replaceChildren(elemento('p', 'suave', error.message));
    $('tabla-admin').replaceChildren(filaMensaje(error.message, 5));
    return;
  }
  pintarModalidades();
  pintarOpcionesEquipo();
  pintarAdmin();
  pintarJuegos();
}

function pintarModalidades() {
  const grupos = new Map();
  for (const equipo of equipos) {
    if (!grupos.has(equipo.modalidad)) {
      grupos.set(equipo.modalidad, []);
    }
    grupos.get(equipo.modalidad).push(equipo);
  }
  $('dato-equipos').textContent = equipos.length;
  const tarifas = equipos.map((e) => e.tarifaMediaHoraPesos);
  $('dato-desde').textContent = tarifas.length ? pesos.format(Math.min(...tarifas)) : '–';

  const lista = $('lista-modalidades');
  lista.replaceChildren();
  for (const [modalidad, unidades] of grupos) {
    const id = clave(modalidad);
    const libres = unidades.filter((e) => e.sePuedeReservar);
    const tarjeta = elemento('li', 'modalidad');

    const imagen = elemento('div', 'modalidad__imagen');
    const icono = elemento('span', '', '🎮');
    icono.setAttribute('aria-hidden', 'true');
    imagen.append(icono);
    if (FOTOS[id] || ILUSTRACIONES.includes(id)) {
      const foto = document.createElement('img');
      foto.className = FOTOS[id] ? 'foto' : 'ilustracion';
      foto.alt = (FOTOS[id] ? 'Foto de ' : 'Ilustración de ') + modalidad + ' en Game Spot';
      foto.addEventListener('error', () => foto.remove());
      foto.src = 'img/' + (FOTOS[id] || 'consola-' + id + '.svg');
      imagen.append(foto);
    }

    if (NIVELES[id]) {
      imagen.append(elemento('span', 'modalidad__nivel', NIVELES[id]));
    }

    const cuerpo = elemento('div', 'modalidad__cuerpo');
    const precio = elemento('p', 'modalidad__precio', pesos.format(unidades[0].tarifaHoraPesos) + ' ');
    precio.append(elemento('small', '', 'la hora'));
    const media = elemento('p', 'modalidad__media', pesos.format(unidades[0].tarifaMediaHoraPesos) + ' ');
    media.append(elemento('small', '', 'la media hora'));
    cuerpo.append(
      elemento('h3', '', modalidad),
      elemento('p', 'suave', DESCRIPCIONES[id] || 'Disponible para reservar por bloques de 30 minutos.'),
      precio,
      media,
    );
    const ahorro = 2 * unidades[0].tarifaMediaHoraPesos - unidades[0].tarifaHoraPesos;
    if (ahorro > 0) {
      cuerpo.append(elemento('p', 'modalidad__ahorro', 'Ahorras ' + pesos.format(ahorro) + ' con la hora completa'));
    }
    cuerpo.append(
      elemento(
        'p',
        libres.length ? 'ok' : 'caido',
        unidades.length + (unidades.length === 1 ? ' equipo' : ' equipos') + ' · ' +
          (libres.length ? libres.length + ' en servicio' : 'fuera de servicio'),
      ),
    );
    const accion = boton('Reservar ' + modalidad, '', () => {
      elegirEquipo(libres[0].id);
      $('reservar').scrollIntoView();
    });
    accion.disabled = libres.length === 0;
    cuerpo.append(accion);
    const verJuegos = elemento('a', 'modalidad__juegos', 'Ver juegos de ' + modalidad);
    verJuegos.href = '#juegos';
    verJuegos.addEventListener('click', () => {
      pestanaJuegos = unidades[0].nombre;
      pintarJuegos();
    });
    cuerpo.append(verJuegos);
    tarjeta.append(imagen, cuerpo);
    lista.append(tarjeta);
  }
  if (!equipos.length) {
    lista.append(elemento('li', 'suave', 'No hay equipos registrados.'));
  }
}

function pintarOpcionesEquipo() {
  const contenedor = $('opciones-equipo');
  contenedor.replaceChildren();
  for (const equipo of equipos) {
    const opcion = elemento('button', 'opcion-equipo' + (equipo.id === reserva.equipoId ? ' elegida' : ''));
    opcion.type = 'button';
    opcion.disabled = !equipo.sePuedeReservar;
    opcion.append(
      elemento('strong', '', equipo.nombre),
      elemento('span', '', equipo.modalidad),
      elemento('span', 'precio', pesos.format(equipo.tarifaHoraPesos) + ' la hora'),
      elemento('span', '', pesos.format(equipo.tarifaMediaHoraPesos) + ' la media hora'),
    );
    const juegos = textoCantidadJuegos(equipo.nombre);
    if (juegos) {
      opcion.append(elemento('span', 'juegos', '🎮 ' + juegos));
    }
    if (!equipo.sePuedeReservar) {
      opcion.append(elemento('span', 'caido', 'Fuera de servicio'));
    }
    opcion.addEventListener('click', () => elegirEquipo(equipo.id));
    contenedor.append(opcion);
  }
}

function pintarAdmin() {
  const tabla = $('tabla-admin');
  tabla.replaceChildren();
  for (const equipo of equipos) {
    const fila = document.createElement('tr');
    fila.append(
      celda(equipo.nombre),
      celda(equipo.modalidad),
      celda(pesos.format(equipo.tarifaHoraPesos) + ' / ' + pesos.format(equipo.tarifaMediaHoraPesos)),
      celda(equipo.sePuedeReservar ? 'Disponible' : 'Fuera de servicio'),
    );
    const acciones = document.createElement('td');
    acciones.append(
      boton(
        equipo.estaDisponible ? 'Poner fuera de servicio' : 'Habilitar',
        equipo.estaDisponible ? 'boton--peligro boton--pequeno' : 'boton--secundario boton--pequeno',
        () => cambiarEstadoEquipo(equipo),
      ),
    );
    fila.append(acciones);
    tabla.append(fila);
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

// ---------- Horario (proveedor de catálogo) ----------
function horaLegible(texto) {
  const [h, m] = texto.split(':').map(Number);
  const sufijo = h < 12 ? 'a. m.' : 'p. m.';
  return ((h + 11) % 12) + 1 + ':' + String(m).padStart(2, '0') + ' ' + sufijo;
}

async function cargarHorarios() {
  const lista = $('lista-horario');
  let horarios;
  try {
    horarios = await api('/api/portal/horarios');
  } catch (error) {
    lista.replaceChildren(elemento('dt', 'suave', error.message));
    return;
  }
  // Agrupa los días seguidos que tienen el mismo horario (por ejemplo, lunes a viernes).
  const grupos = [];
  for (const dia of horarios) {
    const horas = horaLegible(dia.horaApertura) + ' – ' + horaLegible(dia.horaCierre);
    const ultimo = grupos[grupos.length - 1];
    if (ultimo && ultimo.horas === horas && ultimo.hasta === dia.diaSemana - 1) {
      ultimo.hasta = dia.diaSemana;
    } else {
      grupos.push({ desde: dia.diaSemana, hasta: dia.diaSemana, horas });
    }
  }
  lista.replaceChildren();
  for (const grupo of grupos) {
    const fila = elemento('div', 'horario__fila');
    const dias = grupo.desde === grupo.hasta ? DIAS[grupo.desde] : DIAS[grupo.desde] + ' a ' + DIAS[grupo.hasta].toLowerCase();
    fila.append(elemento('dt', '', dias), elemento('dd', '', grupo.horas));
    lista.append(fila);
  }
}

// ---------- Reserva guiada (operaciones integradas) ----------
function hoyEnBogota() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA }).format(new Date());
}

function equipoElegido() {
  return equipos.find((e) => e.id === reserva.equipoId);
}

function irAPaso(numero) {
  pasoActual = numero;
  for (let paso = 1; paso <= 4; paso++) {
    $('paso-' + paso).hidden = paso !== numero;
  }
  document.querySelectorAll('.progreso li').forEach((item) => {
    const paso = Number(item.dataset.paso);
    item.classList.toggle('actual', paso === numero);
    item.classList.toggle('hecho', paso < numero);
    if (paso === numero) {
      item.setAttribute('aria-current', 'step');
    } else {
      item.removeAttribute('aria-current');
    }
  });
}

document.querySelectorAll('[data-ir-a]').forEach((control) =>
  control.addEventListener('click', () => {
    const paso = Number(control.dataset.irA);
    if (paso === 1) {
      reserva.creada = null;
      pintarOpcionesEquipo();
    }
    irAPaso(paso);
  }),
);

function elegirEquipo(id) {
  ocultarAviso();
  reserva.equipoId = id;
  reserva.inicio = null;
  reserva.creada = null;
  const equipo = equipoElegido();
  $('equipo-elegido').textContent = equipo
    ? 'Consola elegida: ' + equipo.nombre + ' (' + pesos.format(equipo.tarifaHoraPesos) + ' la hora, ' +
      pesos.format(equipo.tarifaMediaHoraPesos) + ' la media hora)'
    : '';
  pintarOpcionesEquipo();
  irAPaso(2);
  cargarDisponibilidad();
}

$('res-fecha').addEventListener('change', () => {
  reserva.inicio = null;
  cargarDisponibilidad();
});

$('res-duracion').addEventListener('change', () => {
  reserva.duracionMinutos = Number($('res-duracion').value);
  pintarPrecioPrevio();
});

function calcularPrecio(equipo, minutos) {
  // Misma regla del proveedor de reservas (CACS-30): cada hora completa a la tarifa por hora y la
  // media hora que sobre a su propia tarifa; el adelanto es el 50 %.
  const horasCompletas = Math.floor(minutos / 60);
  const sobraMediaHora = minutos % 60 !== 0;
  const total = horasCompletas * equipo.tarifaHoraPesos + (sobraMediaHora ? equipo.tarifaMediaHoraPesos : 0);
  return { total, adelanto: Math.ceil((total * PORCENTAJE_ADELANTO) / 100) };
}

function pintarPrecioPrevio() {
  const equipo = equipoElegido();
  $('boton-continuar').disabled = !reserva.inicio;
  if (!equipo || !reserva.inicio) {
    $('precio-previo').textContent = reserva.inicio ? '' : 'Toca un bloque libre para elegir la hora de inicio.';
    return;
  }
  const precio = calcularPrecio(equipo, reserva.duracionMinutos);
  $('precio-previo').textContent =
    'Desde las ' + hora.format(new Date(reserva.inicio)) + ' · Total ' + pesos.format(precio.total) +
    ' · Adelanto ' + pesos.format(precio.adelanto);
}

async function cargarDisponibilidad() {
  const contenedor = $('bloques');
  contenedor.replaceChildren(elemento('p', 'suave', 'Consultando disponibilidad…'));
  reserva.fecha = $('res-fecha').value;
  pintarPrecioPrevio();
  if (!reserva.equipoId || !reserva.fecha) {
    contenedor.replaceChildren(elemento('p', 'suave', 'Elige un día.'));
    return;
  }
  try {
    const disponibilidad = await api(
      '/api/portal/equipos/' + reserva.equipoId + '/disponibilidad?fecha=' + reserva.fecha,
    );
    contenedor.replaceChildren();
    if (!disponibilidad.atiende) {
      contenedor.append(elemento('p', 'suave', 'El local no atiende ese día.'));
      return;
    }
    for (const bloque of disponibilidad.bloques) {
      const opcion = elemento('button', 'bloque ' + (bloque.estaLibre ? 'libre' : 'no-libre'));
      opcion.type = 'button';
      opcion.disabled = !bloque.estaLibre;
      opcion.append(
        hora.format(new Date(bloque.inicio)),
        elemento('small', '', bloque.estaLibre ? 'Libre' : MOTIVOS[bloque.motivo] || bloque.motivo),
      );
      if (bloque.inicio === reserva.inicio) {
        opcion.classList.add('elegido');
      }
      opcion.addEventListener('click', () => {
        contenedor.querySelectorAll('.elegido').forEach((b) => b.classList.remove('elegido'));
        opcion.classList.add('elegido');
        reserva.inicio = bloque.inicio;
        pintarPrecioPrevio();
      });
      contenedor.append(opcion);
    }
    if (!disponibilidad.bloques.some((b) => b.estaLibre)) {
      contenedor.append(elemento('p', 'suave', 'No quedan bloques libres ese día; prueba otra fecha.'));
    }
  } catch (error) {
    contenedor.replaceChildren(elemento('p', 'caido', error.message));
  }
}

$('boton-continuar').addEventListener('click', () => {
  if (!reserva.inicio) {
    mostrarAviso('Elige la hora de inicio tocando un bloque libre.');
    return;
  }
  irAPaso(3);
  pintarResumen();
});

function pintarResumen() {
  const equipo = equipoElegido();
  const actual = sesion();
  const resumen = $('resumen');
  resumen.replaceChildren();
  if (!equipo || !reserva.inicio) {
    irAPaso(1);
    return;
  }
  const inicio = new Date(reserva.inicio);
  const fin = new Date(inicio.getTime() + reserva.duracionMinutos * 60000);
  const precio = calcularPrecio(equipo, reserva.duracionMinutos);
  const filas = [
    ['A nombre de', actual ? actual.nombre + ' (' + actual.email + ')' : 'Ingresa para confirmar'],
    ['Consola', equipo.nombre + ' · ' + equipo.modalidad],
    ['Día', fechaLarga.format(inicio)],
    ['Hora', hora.format(inicio) + ' a ' + hora.format(fin)],
    ['Precio total', pesos.format(precio.total)],
    ['Adelanto (50 %)', pesos.format(precio.adelanto)],
  ];
  for (const [titulo, valor] of filas) {
    const dd = elemento('dd', titulo === 'Adelanto (50 %)' ? 'total' : '', valor);
    resumen.append(elemento('dt', '', titulo), dd);
  }
  $('confirmar-login').hidden = Boolean(actual);
  $('boton-confirmar').disabled = !actual;
}

$('boton-confirmar').addEventListener('click', async () => {
  ocultarAviso();
  if (!sesion()) {
    abrirLogin();
    return;
  }
  const boton = $('boton-confirmar');
  boton.disabled = true;
  try {
    const creada = await api('/api/portal/reservas', {
      method: 'POST',
      conToken: true,
      body: {
        equipoId: reserva.equipoId,
        fecha: reserva.fecha,
        hora: hora24.format(new Date(reserva.inicio)),
        duracionMinutos: reserva.duracionMinutos,
      },
    });
    reserva.creada = creada;
    $('texto-exito').textContent =
      creada.nombreEquipo + ', ' + fechaHora.format(new Date(creada.inicio)) + ' a nombre de ' +
      sesion().nombre + '. Total ' + pesos.format(creada.precioTotalPesos) + '. Paga el adelanto de ' +
      pesos.format(creada.adelantoPesos) + ' para confirmarla.';
    $('boton-pagar-ahora').hidden = false;
    irAPaso(4);
    cargarMisReservas();
  } catch (error) {
    mostrarAviso(error.message);
    if (error.status === 401) {
      cerrarSesion();
      abrirLogin();
    } else if (error.status === 409) {
      irAPaso(2);
      reserva.inicio = null;
      cargarDisponibilidad();
    }
  } finally {
    boton.disabled = !sesion();
  }
});

$('boton-pagar-ahora').addEventListener('click', async () => {
  if (reserva.creada) {
    const pagada = await cambiarEstadoReserva(reserva.creada.id, 'pago');
    if (pagada) {
      $('texto-exito').textContent =
        '¡Te esperamos! Tu turno en ' + pagada.nombreEquipo + ' quedó confirmado para el ' +
        fechaHora.format(new Date(pagada.inicio));
      $('boton-pagar-ahora').hidden = true;
    }
  }
});

$('boton-portada-reservar').addEventListener('click', () => {
  reserva.creada = null;
  irAPaso(reserva.equipoId ? 2 : 1);
});

// ---------- Mis reservas (proveedor de reservas) ----------
async function cargarMisReservas() {
  const lista = $('lista-reservas');
  if (!sesion()) {
    lista.replaceChildren();
    return;
  }
  let reservas;
  try {
    reservas = await api('/api/portal/reservas/mias', { conToken: true });
  } catch (error) {
    if (error.status === 401) {
      // CACS-31: el token venció (dura 2 horas) o ya no es válido: se cierra la sesión.
      cerrarSesion();
      mostrarAviso('Tu sesión venció. Ingresa de nuevo para ver y hacer reservas.');
      return;
    }
    lista.replaceChildren(elemento('li', 'caido', error.message));
    return;
  }
  lista.replaceChildren();
  if (reservas.length === 0) {
    lista.append(elemento('li', 'suave', 'Aún no tienes reservas. ¡Haz la primera!'));
    return;
  }
  for (const item of reservas) {
    const tarjeta = elemento('li', 'reserva');
    const encabezado = elemento('div', 'reserva__encabezado');
    encabezado.append(
      elemento('h3', '', item.nombreEquipo),
      elemento('span', 'etiqueta ' + item.estado, ETIQUETAS[item.estado] || item.estado),
    );
    tarjeta.append(
      encabezado,
      elemento('p', '', fechaHora.format(new Date(item.inicio)) + ' – ' + hora.format(new Date(item.fin))),
      elemento('p', '', 'Total ' + pesos.format(item.precioTotalPesos) + ' · Adelanto ' + pesos.format(item.adelantoPesos)),
    );
    const acciones = elemento('div', 'acciones');
    if (item.estado === 'PENDIENTE_PAGO') {
      acciones.append(boton('Pagar adelanto', 'boton--pequeno', () => cambiarEstadoReserva(item.id, 'pago')));
    }
    if (item.estado !== 'CANCELADA') {
      acciones.append(boton('Cancelar', 'boton--peligro boton--pequeno', () => cambiarEstadoReserva(item.id, 'cancelacion')));
    }
    if (acciones.children.length) {
      tarjeta.append(acciones);
    }
    lista.append(tarjeta);
  }
}

async function cambiarEstadoReserva(id, accion) {
  ocultarAviso();
  try {
    const actualizada = await api('/api/portal/reservas/' + id + '/' + accion, { method: 'PATCH', conToken: true });
    mostrarAviso(
      'Reserva ' + (accion === 'pago' ? 'confirmada' : 'cancelada') + ': ' + actualizada.nombreEquipo + '.',
      true,
    );
    cargarMisReservas();
    if (pasoActual === 2) {
      cargarDisponibilidad();
    }
    return actualizada;
  } catch (error) {
    mostrarAviso(error.message);
    return null;
  }
}

// ---------- Ayudas de tabla ----------
function celda(texto) {
  return elemento('td', '', texto);
}

function filaMensaje(texto, columnas) {
  const fila = document.createElement('tr');
  const td = elemento('td', 'suave', texto);
  td.colSpan = columnas;
  fila.append(td);
  return fila;
}

// ---------- Juegos disponibles (CACS-32) ----------
async function cargarCatalogoJuegos() {
  try {
    const respuesta = await fetch('datos/juegos.json');
    const datos = await respuesta.json();
    catalogoJuegos = datos.equipos || {};
    caratulas = datos.caratulas || {};
  } catch {
    catalogoJuegos = {};
  }
  pintarMuroCaratulas();
  const totales = Object.values(catalogoJuegos).map((c) => c.totalJuegos || 0);
  if (totales.length) {
    $('dato-juegos').textContent = Math.max(...totales) + '+';
  }
  pintarJuegos();
  pintarOpcionesEquipo();
}

function contarJuegos(catalogo) {
  return catalogo.categorias.reduce((total, categoria) => total + categoria.juegos.length, 0);
}

function textoCantidadJuegos(nombreEquipo) {
  const catalogo = catalogoJuegos[nombreEquipo];
  if (!catalogo) {
    return '';
  }
  return (catalogo.totalJuegos || contarJuegos(catalogo) + '+') + ' juegos';
}

function pintarJuegos() {
  const pestanas = $('pestanas-juegos');
  const panel = $('panel-juegos');
  const nombres = equipos.length ? equipos.map((e) => e.nombre) : Object.keys(catalogoJuegos);
  if (!nombres.length || !Object.keys(catalogoJuegos).length) {
    pestanas.replaceChildren();
    panel.replaceChildren(elemento('p', 'suave', 'Pregunta en el local por los juegos de cada consola.'));
    return;
  }
  if (!nombres.includes(pestanaJuegos)) {
    pestanaJuegos = nombres[0];
  }
  pestanas.replaceChildren();
  for (const nombre of nombres) {
    const pestana = elemento('button', 'pestana' + (nombre === pestanaJuegos ? ' activa' : ''), nombre);
    pestana.type = 'button';
    pestana.setAttribute('role', 'tab');
    pestana.setAttribute('aria-selected', String(nombre === pestanaJuegos));
    pestana.addEventListener('click', () => {
      pestanaJuegos = nombre;
      pintarJuegos();
    });
    pestanas.append(pestana);
  }

  const catalogo = catalogoJuegos[pestanaJuegos];
  panel.replaceChildren();
  if (!catalogo) {
    panel.append(elemento('p', 'suave', 'Pregunta en el local por los juegos de ' + pestanaJuegos + '.'));
    return;
  }
  panel.append(elemento('p', 'panel-juegos__nota', textoCantidadJuegos(pestanaJuegos) + ' · ' + catalogo.nota));
  const columnas = elemento('div', 'categorias');
  for (const categoria of catalogo.categorias) {
    const bloque = elemento('div', 'categoria');
    bloque.append(elemento('h3', '', categoria.nombre + ' (' + categoria.juegos.length + ')'));
    const lista = elemento('ul', 'lista-juegos');
    for (const [titulo, jugadores] of categoria.juegos) {
      const item = elemento('li', '');
      const nombre = elemento('span', 'juego__nombre');
      if (caratulas[titulo]) {
        const mini = document.createElement('img');
        mini.className = 'juego__caratula';
        mini.src = 'img/caratulas/' + caratulas[titulo];
        mini.alt = '';
        mini.loading = 'lazy';
        nombre.append(mini);
      }
      nombre.append(titulo);
      item.append(nombre);
      if (jugadores > 1) {
        const etiqueta = elemento('span', 'jugadores', '👥 ' + jugadores);
        etiqueta.title = 'Hasta ' + jugadores + ' jugadores en la misma consola';
        item.append(etiqueta);
      }
      lista.append(item);
    }
    bloque.append(lista);
    columnas.append(bloque);
  }
  panel.append(columnas);
}

// Muro de carátulas que se desplaza detrás de la sección de juegos (CACS-36).
function pintarMuroCaratulas() {
  const muro = $('muro-caratulas');
  const archivos = Object.values(caratulas);
  muro.replaceChildren();
  if (!archivos.length) {
    return;
  }
  for (let fila = 0; fila < 2; fila++) {
    const cinta = elemento('div', 'muro-caratulas__fila' + (fila ? ' muro-caratulas__fila--inversa' : ''));
    const orden = fila ? [...archivos].reverse() : archivos;
    // Se repite la lista para que el desplazamiento no tenga cortes.
    for (const archivo of [...orden, ...orden]) {
      const img = document.createElement('img');
      img.src = 'img/caratulas/' + archivo;
      img.alt = '';
      img.loading = 'lazy';
      cinta.append(img);
    }
    muro.append(cinta);
  }
}

// ---------- Videos del local (CACS-32) ----------
// CACS-40: se reproducen solos y sin sonido mientras se ven en pantalla, también con "reducir
// movimiento" (el equipo lo pidió así). Cada uno tiene su botón para pausarlo, y si el visitante
// lo pausa, no vuelve a arrancar solo.
function prepararVideos() {
  const videos = document.querySelectorAll('.videos-local video');
  for (const video of videos) {
    // Los navegadores solo permiten la reproducción automática de videos sin sonido.
    video.muted = true;
    // CACS-36: botón visible para reproducir o pausar; el video siempre muestra su portada.
    const boton = elemento('button', 'video__play', '▶');
    boton.type = 'button';
    const titulo = video.parentElement.querySelector('figcaption')?.textContent || 'video';
    const actualizar = () => {
      boton.textContent = video.paused ? '▶' : '❚❚';
      boton.setAttribute('aria-label', (video.paused ? 'Reproducir ' : 'Pausar ') + titulo);
      boton.classList.toggle('reproduciendo', !video.paused);
    };
    const alternar = () => {
      video.dataset.pausadoPorUsuario = String(!video.paused);
      return video.paused ? video.play().catch(() => {}) : video.pause();
    };
    boton.addEventListener('click', alternar);
    video.addEventListener('click', alternar);
    video.addEventListener('play', actualizar);
    video.addEventListener('pause', actualizar);
    video.parentElement.append(boton);
    actualizar();
  }
  if (!('IntersectionObserver' in window)) {
    videos.forEach((video) => video.play().catch(() => {}));
    return;
  }
  const observador = new IntersectionObserver(
    (entradas) => {
      for (const entrada of entradas) {
        if (entrada.isIntersecting) {
          if (entrada.target.dataset.pausadoPorUsuario !== 'true') {
            entrada.target.play().catch(() => {});
          }
        } else {
          entrada.target.pause();
        }
      }
    },
    { threshold: 0.4 },
  );
  videos.forEach((video) => observador.observe(video));
}

// ---------- Reto 10 segundos, versión de práctica (CACS-35) ----------
// Solo es una práctica en el navegador: no da descuentos. El reto válido se hace en el local.
const reto = { corriendo: false, inicio: 0, cuadro: 0 };

function formatoReto(segundos) {
  return segundos.toFixed(2).padStart(5, '0');
}

// CACS-43: el gato del reto comenta cada intento y da un brinco al detener el cronómetro.
function gatoDice(texto, reacciona) {
  const globo = $('reto-gato-dice');
  globo.textContent = texto;
  const gato = globo.closest('.reto__gato');
  gato.classList.remove('reto__gato--reacciona');
  if (reacciona) {
    // Leer el ancho obliga al navegador a reiniciar la animación del brinco.
    void gato.offsetWidth;
    gato.classList.add('reto__gato--reacciona');
  }
}

function pintarCronometro() {
  $('reto-tiempo').textContent = formatoReto((performance.now() - reto.inicio) / 1000);
  reto.cuadro = requestAnimationFrame(pintarCronometro);
}

$('reto-boton').addEventListener('click', () => {
  const boton = $('reto-boton');
  if (!reto.corriendo) {
    reto.corriendo = true;
    reto.inicio = performance.now();
    boton.textContent = '¡Detener!';
    $('reto-resultado').textContent = 'Corre el tiempo… detenlo en 10,00.';
    gatoDice('👀 No parpadeo…', false);
    reto.cuadro = requestAnimationFrame(pintarCronometro);
    return;
  }
  reto.corriendo = false;
  cancelAnimationFrame(reto.cuadro);
  const segundos = Math.round((performance.now() - reto.inicio) / 10) / 100;
  $('reto-tiempo').textContent = formatoReto(segundos);
  boton.textContent = 'Intentar de nuevo';
  const diferencia = Math.round((segundos - 10) * 100) / 100;
  const texto = Math.abs(diferencia).toFixed(2).replace('.', ',');
  let mensaje;
  if (diferencia === 0) {
    mensaje = '¡10,00 exactos! Ven el fin de semana y hazlo en el local para pagar la mitad.';
    gatoDice('¡Miau! ¡Perfecto! 😼', true);
  } else {
    const casi = Math.abs(diferencia) <= 0.3;
    const animo = casi ? '¡Casi lo logras!' : 'Sigue practicando.';
    mensaje = (diferencia < 0 ? 'Te faltaron ' : 'Te pasaste por ') + texto + ' s. ' + animo;
    gatoDice(casi ? '¡Uy, casi! Otra vez.' : 'Mmm… yo te espero.', true);
  }
  $('reto-resultado').textContent = mensaje;
});

// ---------- Inicio ----------
$('boton-estado').addEventListener('click', () => {
  revisarEstado();
  cargarEquipos();
  cargarHorarios();
});
if (FOTOS.local) {
  $('inicio').style.setProperty('--foto-portada', "url('img/" + FOTOS.local + "')");
}
$('res-fecha').value = hoyEnBogota();
$('res-fecha').min = hoyEnBogota();
irAPaso(1);
pintarSesion();
revisarEstado();
cargarEquipos();
cargarHorarios();
cargarCatalogoJuegos();
prepararVideos();
