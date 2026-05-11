import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  registerForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onRegister() {
    if (this.registerForm.valid) {
      this.authService.registrar(this.registerForm.value).subscribe({
        next: () => {
          alert('¡Cuenta creada! Ya podés loguearte.');
          this.router.navigate(['/login']);
        },
        error: (err: any) => {
          console.error('Error en el registro', err);
          alert('No se pudo crear la cuenta. ¿Quizás el email ya existe?');
        }
      });
    }
  }
}
