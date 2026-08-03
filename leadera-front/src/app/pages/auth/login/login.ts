import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  loginForm: FormGroup;
  errorLogin = signal<string>('');

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  formHovering = signal(false);

  onFormMouseMove(event: MouseEvent) {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const x = event.clientX - rect.left - 250;
    const y = event.clientY - rect.top - 250;
    el.style.setProperty('--ax', `${x}px`);
    el.style.setProperty('--ay', `${y}px`);
  }

  onInputGlow(event: MouseEvent) {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const x = event.clientX - rect.left;
    el.style.setProperty('--mx', `${x}px`);
  }

  onSubmit(){
    if(this.loginForm.valid){
      this.authService.login(this.loginForm.value).subscribe({
        next: (res) => {
          this.errorLogin.set('');
          // Password temporal asignada por el dueño: obligamos a cambiarla
          // antes de usar el resto de la app (el authGuard refuerza esto).
          if (res.debeCambiarPassword) {
            this.router.navigate(['/cambiar-password']);
          } else {
            this.router.navigate(['/home']);
          }
        },
        error: (err) => {
          this.errorLogin.set(err.mensajeAmigable || 'Credenciales incorrectas');
        }
      })
    }
  }
}
