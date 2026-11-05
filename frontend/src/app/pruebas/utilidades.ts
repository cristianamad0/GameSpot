import { RespuestaAutenticacion, Rol } from '../core/modelos';

/** JWT de prueba (sin firma válida) que vence en {@code segundos} segundos. */
export function tokenDePrueba(segundos = 3600): string {
  const base64Url = (objeto: object) =>
    btoa(JSON.stringify(objeto)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const exp = Math.floor(Date.now() / 1000) + segundos;
  return `${base64Url({ alg: 'HS384' })}.${base64Url({ sub: 'x@gamespot.test', exp })}.firma`;
}

export function respuestaDePrueba(rol: Rol = 'CLIENTE', segundos = 3600): RespuestaAutenticacion {
  return {
    token: tokenDePrueba(segundos),
    id: 1,
    nombre: rol === 'ADMIN' ? 'Admin Demo' : 'Cliente Demo',
    email: rol === 'ADMIN' ? 'admin@gamespot.test' : 'cliente@gamespot.test',
    rol,
  };
}
