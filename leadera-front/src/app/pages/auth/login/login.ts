import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth-service';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [RouterModule, ReactiveFormsModule],
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
