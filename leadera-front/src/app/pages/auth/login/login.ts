import { Component } from '@angular/core';
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
        next: (response) => {
          console.log('Response completa:', response);
          localStorage.setItem('token', response.token);
          localStorage.setItem('agente_nombre', response.nombre); 
          localStorage.setItem('agente_email', response.email);  
          console.log('Login exitoso, token guardado');
          this.router.navigate(['/home']);
        },
        error: (err) => { 
          console.error('Error en el login', err);
          alert('Credenciales incorrectas. ¡Fijate bien!');
        }
      })
    }
  }
}
