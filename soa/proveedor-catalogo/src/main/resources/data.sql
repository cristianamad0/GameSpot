-- CACS-25: DML con los datos reales del negocio (entregados por el propietario
-- el 8/10/2026). Cada insert solo se ejecuta si el dato no existe, para no
-- duplicar ni pisar los cambios hechos desde la API al reiniciar.

insert into modalidades (nombre, tarifa_hora_pesos)
select 'Xbox 360', 5000 where not exists (select 1 from modalidades where nombre = 'Xbox 360');
insert into modalidades (nombre, tarifa_hora_pesos)
select 'Xbox Series S', 6000 where not exists (select 1 from modalidades where nombre = 'Xbox Series S');
insert into modalidades (nombre, tarifa_hora_pesos)
select 'Sala VIP Xbox Series X', 7000
where not exists (select 1 from modalidades where nombre = 'Sala VIP Xbox Series X');

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
select 'Xbox Series S #3', id from modalidades where nombre = 'Xbox Series S'
and not exists (select 1 from equipos where nombre = 'Xbox Series S #3');
insert into equipos (nombre, modalidad_id)
select 'Xbox Series S #4', id from modalidades where nombre = 'Xbox Series S'
and not exists (select 1 from equipos where nombre = 'Xbox Series S #4');
insert into equipos (nombre, modalidad_id)
select 'Sala VIP Xbox Series X', id from modalidades where nombre = 'Sala VIP Xbox Series X'
and not exists (select 1 from equipos where nombre = 'Sala VIP Xbox Series X');

-- Lunes a viernes de 14:00 a 22:00; sábado y domingo de 15:00 a 21:00.
merge into horarios_atencion (dia_semana, hora_apertura, hora_cierre) key (dia_semana) values
  (1, '14:00:00', '22:00:00'), (2, '14:00:00', '22:00:00'), (3, '14:00:00', '22:00:00'),
  (4, '14:00:00', '22:00:00'), (5, '14:00:00', '22:00:00'),
  (6, '15:00:00', '21:00:00'), (7, '15:00:00', '21:00:00');
