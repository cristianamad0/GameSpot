-- =====================================================================
-- Borra TODAS las tablas de Game Spot y sus datos.
--
-- Uso: solo en Supabase, en el SQL Editor, antes de volver a ejecutar
-- esquema.sql cuando el esquema cambia y todavía no hay datos reales.
-- NO usar cuando ya existan reservas o pagos de clientes reales.
-- =====================================================================

drop view if exists public.saldo_puntos;

drop table if exists
  public.notificaciones,
  public.movimientos_puntos,
  public.pagos,
  public.reservas,
  public.configuracion_negocio,
  public.horarios_atencion,
  public.equipos,
  public.modalidades,
  public.usuarios
cascade;

drop function if exists public.expirar_reservas_pendientes();
drop function if exists public.confirmar_reserva_al_aprobar_pago();
drop function if exists public.otorgar_puntos_al_completar();
drop function if exists public.actualizar_puntos_fidelidad();
drop function if exists public.marcar_actualizado_en();
