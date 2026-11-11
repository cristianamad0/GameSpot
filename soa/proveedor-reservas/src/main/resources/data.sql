-- CACS-26: DML con los usuarios de prueba. Son cuentas de demostración, no
-- credenciales reales: contraseñas AdminDemo123 y ClienteDemo123 (ver README),
-- guardadas como hash BCrypt. Solo se insertan si no existen.

insert into usuarios (nombre, email, password_hash, rol)
select 'Administrador Game Spot', 'admin@gamespot.demo',
       '$2a$10$LK4TYCwsD7hg7.kfb8yX9uCcXzqh1NviaB0L0663clbgfbkC5k5HG', 'ADMIN'
where not exists (select 1 from usuarios where email = 'admin@gamespot.demo');

insert into usuarios (nombre, email, password_hash, rol)
select 'Laura Gómez', 'laura@gamespot.demo',
       '$2a$10$fukDNqjyuQ2ULTv9UafYmOSWYrB7xKQO4Vsb2.atKF9bMY9jItApu', 'CLIENTE'
where not exists (select 1 from usuarios where email = 'laura@gamespot.demo');

insert into usuarios (nombre, email, password_hash, rol)
select 'Andrés Rojas', 'andres@gamespot.demo',
       '$2a$10$fukDNqjyuQ2ULTv9UafYmOSWYrB7xKQO4Vsb2.atKF9bMY9jItApu', 'CLIENTE'
where not exists (select 1 from usuarios where email = 'andres@gamespot.demo');
