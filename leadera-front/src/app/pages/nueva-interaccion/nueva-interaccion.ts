import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Agregalo para pipes/directivas
import { ActivatedRoute, Router } from '@angular/router';
import { InteraccionService, CrearInteraccionRequest } from '../../core/services/interaccion-service';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-nueva-interaccion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './nueva-interaccion.html',
  styleUrl: './nueva-interaccion.css',
})
export class NuevaInteraccion implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private interaccionService = inject(InteraccionService);

  private leadId!: number;

  public miFormulario: FormGroup = this.fb.group({
    tipo: ['', [Validators.required]],
    detalle: ['', [Validators.required, Validators.minLength(10)]],
    fechaProximoContacto: [''] 
  });

 setSeguimiento(dias: number) {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + dias);

  const year = fecha.getFullYear();
  const month = String(fecha.getMonth() + 1).padStart(2, '0');
  const day = String(fecha.getDate()).padStart(2, '0');
  const hours = String(fecha.getHours()).padStart(2, '0');
  const minutes = String(fecha.getMinutes()).padStart(2, '0');

  const fechaFormateada = `${year}-${month}-${day}T${hours}:${minutes}`;

  this.miFormulario.get('fechaProximoContacto')?.setValue(fechaFormateada);
}

  ngOnInit(): void {
    // 1. Obtenemos el ID del lead desde la URL (ej: /leads/5/nueva-interaccion)
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.leadId = Number(id);
    }
  }

  esInvalido(campo: string): boolean | null {
    return this.miFormulario.controls[campo].errors && this.miFormulario.controls[campo].touched;
  }

  guardar() {
    if (this.miFormulario.invalid) {
      this.miFormulario.markAllAsTouched();
      return;
    }

    const { tipo, detalle, fechaProximoContacto } = this.miFormulario.value;

    const nuevaInteraccion: CrearInteraccionRequest = {
      tipoInteraccion: tipo,
      detalle: detalle,
    
    };

    // Fíjate que ahora pasamos el tercer parámetro al servicio
    this.interaccionService.crearInteraccion(this.leadId, nuevaInteraccion, fechaProximoContacto).subscribe({
      next: () => {
        this.router.navigate(['/gestion-del-dia']); // O a donde prefieras volver
      },
      error: (err) => {
        console.error('Error:', err);
      }
    });
  }
}