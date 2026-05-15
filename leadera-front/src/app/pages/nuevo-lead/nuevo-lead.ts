import { Component, inject } from '@angular/core';
import { Validators, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-nuevo-lead',
  imports: [ReactiveFormsModule],
  templateUrl: './nuevo-lead.html',
  styleUrl: './nuevo-lead.css',
})
export class NuevoLead {
  private fb = inject(FormBuilder);
  private leadService = inject(LeadService);
  private router = inject(Router);
  private location = inject(Location); // Inyectamos el servicio

  errorEmail: string | null = null;

  // Creá un método para volver atrás
  volver() {
    this.location.back();
  }

  public leadForm = this.fb.group({
    nombre: ['', [Validators.required]],
    apellido: ['', [Validators.required]],
    telefono: ['', [Validators.required]],
    email: ['', [Validators.email]],
    estado: ['FRIO', [Validators.required]],
    origen: ['MANUAL', [Validators.required]],
    descripcionInicial: ['', [Validators.maxLength(500)]],
    fechaProximoSeguimiento: [''],
  });

  guardar() {
    if (this.leadForm.invalid) return;

    // Enviamos el objeto lead al backend
    this.leadService.crearLead(this.leadForm.value).subscribe({
      next: (leadCreado) => {
        this.router.navigate(['/leads', leadCreado.id]);
      },
      error: (err) => {
        console.log('status:', err.status);
        if (err.status === 409 || err.status === 403) {
          this.errorEmail = 'Ya existe un lead con ese email';
        } else {
          this.errorEmail = 'Error al crear el lead';
        }
      },
    });
  }
}
