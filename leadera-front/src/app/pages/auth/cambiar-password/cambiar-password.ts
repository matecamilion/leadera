import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';
import { NotificationService } from '../../../core/services/notification-service';

// Pantalla de cambio de password. Es la única ruta navegable mientras
// debeCambiarPassword sea true (el authGuard bloquea el resto), pero también
// sirve para un cambio voluntario.
@Component({
  selector: 'app-cambiar-password',
  imports: [ReactiveFormsModule],
  templateUrl: './cambiar-password.html',
  styleUrl: './cambiar-password.css',
})
export class CambiarPassword {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private notificaciones = inject(NotificationService);

  guardando = signal(false);
  errorServidor = signal<string>('');

  // El flujo forzado (password temporal) muestra un texto explicativo extra.
  esCambioForzado = this.authService.debeCambiarPassword();

  form: FormGroup = this.fb.group({
    passwordActual: ['', Validators.required],
    passwordNueva: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
    passwordRepetir: ['', Validators.required],
  }, { validators: [coincidenPasswords, nuevaDistintaDeActual] });

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorServidor.set('');

    const { passwordActual, passwordNueva } = this.form.value;
    this.authService.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificaciones.exito('Contraseña actualizada con éxito.');
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.guardando.set(false);
        this.errorServidor.set(err?.error?.message || 'No se pudo actualizar la contraseña.');
      },
    });
  }

  esInvalido(campo: string): boolean {
    const c = this.form.get(campo);
    return !!(c && c.invalid && c.touched);
  }

  get noCoinciden(): boolean {
    return !!(this.form.hasError('noCoinciden') && this.form.get('passwordRepetir')?.touched);
  }

  get nuevaIgualActual(): boolean {
    return !!(this.form.hasError('nuevaIgualActual') && this.form.get('passwordNueva')?.touched);
  }
}

function coincidenPasswords(group: AbstractControl): ValidationErrors | null {
  const nueva = group.get('passwordNueva')?.value;
  const repetir = group.get('passwordRepetir')?.value;
  return nueva && repetir && nueva !== repetir ? { noCoinciden: true } : null;
}

function nuevaDistintaDeActual(group: AbstractControl): ValidationErrors | null {
  const actual = group.get('passwordActual')?.value;
  const nueva = group.get('passwordNueva')?.value;
  return actual && nueva && actual === nueva ? { nuevaIgualActual: true } : null;
}
