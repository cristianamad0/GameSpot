import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { mensajeDeError } from '../../../core/errores/mensaje-error';
import { textoPuntos } from '../../../core/formato';
import { UsuarioResumen } from '../../../core/modelos';
import { AdminApi } from '../admin-api';

/** Un ajuste de cero puntos no cambia nada y el backend lo rechaza. */
function distintoDeCero(control: AbstractControl): ValidationErrors | null {
  return control.value === 0 ? { cero: true } : null;
}

/**
 * CACS-18 (HU-04): el administrador busca a un cliente por correo y registra un ajuste manual de
 * puntos con descripción. El backend impide que el saldo quede negativo (409).
 */
@Component({
  selector: 'gs-ajuste-puntos',
  imports: [ReactiveFormsModule],
  templateUrl: './ajuste-puntos.html',
  styleUrl: './ajuste-puntos.scss',
})
export class AjustePuntos {
  private readonly api = inject(AdminApi);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly formularioBusqueda = this.fb.group({
    email: ['', [Validators.required, Validators.minLength(3)]],
  });
  protected readonly busqueda = this.formularioBusqueda.controls.email;
  protected readonly resultados = signal<UsuarioResumen[] | null>(null);
  protected readonly buscando = signal(false);
  protected readonly errorBusqueda = signal<string | null>(null);

  protected readonly seleccionado = signal<UsuarioResumen | null>(null);
  protected readonly ajuste = this.fb.group({
    puntos: this.fb.control<number | null>(null, [Validators.required, distintoDeCero]),
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
  });
  protected readonly textoPuntos = textoPuntos;
  protected readonly guardando = signal(false);
  protected readonly errorAjuste = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected buscar(): void {
    if (this.busqueda.invalid) {
      this.busqueda.markAsTouched();
      return;
    }
    this.buscando.set(true);
    this.errorBusqueda.set(null);
    this.api.buscarUsuarios(this.busqueda.value.trim()).subscribe({
      next: (usuarios) => {
        this.resultados.set(usuarios);
        this.buscando.set(false);
      },
      error: (error) => {
        this.errorBusqueda.set(mensajeDeError(error));
        this.buscando.set(false);
      },
    });
  }

  protected elegir(usuario: UsuarioResumen): void {
    this.seleccionado.set(usuario);
    this.ajuste.reset();
    this.errorAjuste.set(null);
    this.aviso.set(null);
  }

  protected invalido(campo: 'puntos' | 'descripcion'): boolean {
    const control = this.ajuste.controls[campo];
    return control.invalid && control.touched;
  }

  protected guardar(): void {
    const usuario = this.seleccionado();
    const { puntos, descripcion } = this.ajuste.getRawValue();
    if (!usuario || this.ajuste.invalid || puntos === null || !descripcion.trim()) {
      this.ajuste.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.errorAjuste.set(null);
    this.aviso.set(null);
    this.api.ajustarPuntos(usuario.id, puntos, descripcion.trim()).subscribe({
      next: (saldo) => {
        const actualizado = { ...usuario, puntosFidelidad: saldo.saldoPuntos };
        this.seleccionado.set(actualizado);
        this.resultados.update(
          (lista) => lista?.map((u) => (u.id === actualizado.id ? actualizado : u)) ?? null,
        );
        this.aviso.set(
          `Ajuste de ${puntos > 0 ? '+' : ''}${textoPuntos(puntos)} registrado. ` +
            `Nuevo saldo de ${usuario.nombre}: ${textoPuntos(saldo.saldoPuntos)}.`,
        );
        this.ajuste.reset();
        this.guardando.set(false);
      },
      error: (error) => {
        this.errorAjuste.set(mensajeDeError(error));
        this.guardando.set(false);
      },
    });
  }
}
