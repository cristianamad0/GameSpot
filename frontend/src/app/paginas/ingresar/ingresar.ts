import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AutenticacionApi } from '../../core/auth/autenticacion-api';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { RespuestaAutenticacion } from '../../core/modelos';

/** CACS-15 (HU-01): inicio de sesión. Al entrar, vuelve a la página pedida o a la de su rol. */
@Component({
  selector: 'gs-ingresar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './ingresar.html',
  styleUrl: '../acceso.scss',
})
export class Ingresar {
  private readonly api = inject(AutenticacionApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrarClave = signal(false);
  protected readonly sesionVencida =
    this.ruta.snapshot.queryParamMap.get('motivo') === 'sesion-vencida';

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.api.ingresar(this.formulario.getRawValue()).subscribe({
      next: (respuesta) => this.irDespuesDeIngresar(respuesta),
      error: (error) => {
        this.error.set(mensajeDeError(error));
        this.enviando.set(false);
      },
    });
  }

  protected invalido(campo: 'email' | 'password'): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && control.touched;
  }

  private irDespuesDeIngresar(respuesta: RespuestaAutenticacion): void {
    const vuelta = this.ruta.snapshot.queryParamMap.get('vuelta');
    const destino = vuelta?.startsWith('/') ? vuelta : respuesta.rol === 'ADMIN' ? '/admin' : '/';
    this.router.navigateByUrl(destino);
  }
}
