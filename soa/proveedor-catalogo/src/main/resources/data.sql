-- CACS-25 y CACS-30: DML con los datos reales del negocio. Equipos y precios
-- corregidos por el propietario el 11/11/2026: la hora y la media hora tienen
-- precios propios. Cada insert solo se ejecuta si el dato no existe, para no
-- duplicar ni pisar los cambios hechos desde la API al reiniciar.

-- 1. Corrección única de las bases creadas antes de CACS-30. Solo actúa
--    mientras alguna modalidad no tenga tarifa de media hora; después de la
--    primera ejecución ya no cambia nada.
delete from equipos
where nombre in ('Xbox Series S #3', 'Xbox Series S #4')
  and exists (select 1 from modalidades where tarifa_media_hora_pesos is null);
update modalidades set tarifa_hora_pesos = 4000, tarifa_media_hora_pesos = 2500
where nombre = 'Xbox 360' and tarifa_media_hora_pesos is null;
update modalidades set tarifa_hora_pesos = 5000, tarifa_media_hora_pesos = 3000
where nombre = 'Xbox Series S' and tarifa_media_hora_pesos is null;
update modalidades set tarifa_hora_pesos = 10000, tarifa_media_hora_pesos = 6000
where nombre = 'Sala VIP Xbox Series X' and tarifa_media_hora_pesos is null;

-- 2. Modalidades con su tarifa por hora y por media hora.
insert into modalidades (nombre, tarifa_hora_pesos, tarifa_media_hora_pesos)
select 'Xbox 360', 4000, 2500
where not exists (select 1 from modalidades where nombre = 'Xbox 360');
insert into modalidades (nombre, tarifa_hora_pesos, tarifa_media_hora_pesos)
select 'Xbox Series S', 5000, 3000
where not exists (select 1 from modalidades where nombre = 'Xbox Series S');
insert into modalidades (nombre, tarifa_hora_pesos, tarifa_media_hora_pesos)
select 'Sala VIP Xbox Series X', 10000, 6000
where not exists (select 1 from modalidades where nombre = 'Sala VIP Xbox Series X');

-- 3. Equipos del local: dos Xbox 360, dos Xbox Series S y la sala VIP.
insert into equipos (nombre, modalidad_id)
select 'Xbox 360 #1', id from modalidades where nombre = 'Xbox 360'
and not exists (select 1 from equipos where nombre = 'Xbox 360 #1');
insert into equipos (nombre, modalidad_id)
select 'Xbox Series S #1', id from modalidades where nombre = 'Xbox Series S'
and not exists (select 1 from equipos where nombre = 'Xbox Series S #1');
insert into equipos (nombre, modalidad_id)
select 'Xbox Series S #2', id from modalidades where nombre = 'Xbox Series S'
and not exists (select 1 from equipos where nombre = 'Xbox Series S #2');
insert into equipos (nombre, modalidad_id)
select 'Sala VIP Xbox Series X', id from modalidades where nombre = 'Sala VIP Xbox Series X'
and not exists (select 1 from equipos where nombre = 'Sala VIP Xbox Series X');
insert into equipos (nombre, modalidad_id)
select 'Xbox 360 #2', id from modalidades where nombre = 'Xbox 360'
and not exists (select 1 from equipos where nombre = 'Xbox 360 #2');

-- CACS-33: todos los días de 15:00 a 21:00 (horario confirmado por el equipo el
-- 11/11/2026, igual al de la publicidad del local). El merge también corrige las
-- bases que ya existen al arrancar.
merge into horarios_atencion (dia_semana, hora_apertura, hora_cierre) key (dia_semana) values
  (1, '15:00:00', '21:00:00'), (2, '15:00:00', '21:00:00'), (3, '15:00:00', '21:00:00'),
  (4, '15:00:00', '21:00:00'), (5, '15:00:00', '21:00:00'),
  (6, '15:00:00', '21:00:00'), (7, '15:00:00', '21:00:00');
