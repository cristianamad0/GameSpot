-- =====================================================================
-- Game Spot Reservas — Migración V2: datos reales del negocio
--
-- Aplica sobre el esquema que ya existe en Supabase los datos entregados por
-- el propietario (8/10/2026). Se ejecuta UNA sola vez en el SQL Editor, el
-- mismo día en que se fusiona el PR. Las bases nuevas no la necesitan:
-- esquema.sql ya trae estos cambios.
--
-- Todo va en una transacción: si un paso falla, no se aplica ninguno.
-- =====================================================================

begin;

-- 1. El adelanto pasa de un valor fijo en pesos a un porcentaje del precio.
--    La restricción anterior conserva su nombre al renombrar la columna, así
--    que se elimina y se crea con el nombre que tendría en esquema.sql.
alter table public.configuracion_negocio
  drop constraint configuracion_negocio_valor_adelanto_pesos_check;

alter table public.configuracion_negocio
  rename column valor_adelanto_pesos to porcentaje_adelanto;

update public.configuracion_negocio
   set porcentaje_adelanto = 50,
       puntos_por_hora = 1
 where id = 1;

alter table public.configuracion_negocio
  add constraint configuracion_negocio_porcentaje_adelanto_check
  check (porcentaje_adelanto between 1 and 100);

-- 2. Equipos reales: la Sala VIP es una sola y hay 4 Xbox Series S.
update public.equipos
   set nombre = 'Sala VIP Xbox Series X'
 where nombre = 'Sala VIP Xbox Series X #1';

insert into public.equipos (modalidad_id, nombre)
select m.id, e.nombre
  from (values
          ('Xbox Series S #2'),
          ('Xbox Series S #3'),
          ('Xbox Series S #4')
       ) as e (nombre)
  cross join public.modalidades m
 where m.nombre = 'Xbox Series S';

-- 3. Horario de atención: lunes a viernes de 14:00 a 22:00; sábado y domingo
--    de 15:00 a 21:00 (1 = lunes ... 7 = domingo).
insert into public.horarios_atencion (dia_semana, hora_apertura, hora_cierre) values
  (1, '14:00', '22:00'), (2, '14:00', '22:00'), (3, '14:00', '22:00'),
  (4, '14:00', '22:00'), (5, '14:00', '22:00'),
  (6, '15:00', '21:00'), (7, '15:00', '21:00');

commit;
