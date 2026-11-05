import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AutenticacionApi } from '../../core/auth/autenticacion-api';
import { mensajeDeError } from '../../core/errores/mensaje-error';

/** Las dos contraseñas deben coincidir. */
function clavesCoinciden(grupo: AbstractControl): ValidationErrors | null {
  const clave = grupo.get('password')?.value;
  const confirmacion = grupo.get('confirmacion')?.value;
  return clave && confirmacion && clave !== confirmacion ? { clavesDistintas: true } : null;
}

/**
 * CACS-15 (HU-01): registro de clientes. Usa las mismas reglas que RegistroRequest del backend
 * (nombre de 2 a 120 caracteres, correo válido y contraseña de al menos 8) y abre la sesión.
 */
@Component({
  selector: 'gs-registro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: '../acceso.scss',
})
export class Registro {
  private readonly api = inject(AutenticacionApi);
  private readonly router = inject(Router);

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmacion: ['', Validators.required],
    },
    { validators: clavesCoinciden },
  );
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrarClave = signal(false);

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { nombre, email, password } = this.formulario.getRawValue();
    this.api.registrar({ nombre: nombre.trim(), email, password }).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error) => {
        this.error.set(mensajeDeError(error));
        this.enviando.set(false);
      },
    });
  }

  protected invalido(campo: 'nombre' | 'email' | 'password' | 'confirmacion'): boolean {
    const control = this.formulario.controls[campo];
    const distintas =
      campo === 'confirmacion' && this.formulario.hasError('clavesDistintas') && control.touched;
    return (control.invalid && control.touched) || distintas;
  }
}
