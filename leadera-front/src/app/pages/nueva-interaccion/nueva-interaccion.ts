import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { InteraccionService, CrearInteraccionRequest } from '../../core/services/interaccion-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';
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
  private leadService = inject(LeadService);

  private leadId!: number;
  lead = signal<Lead | null>(null);

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
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.leadId = Number(id);
      this.leadService.getLeadById(this.leadId).subscribe({
        next: (l) => this.lead.set(l),
        error: () => {}
      });
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
      proximoContacto: fechaProximoContacto || undefined,
    };

    this.interaccionService.crearInteraccion(this.leadId, nuevaInteraccion).subscribe({
      next: () => {
        this.router.navigate(['/gestion-del-dia']); // O a donde prefieras volver
      },
      error: (err) => {
        console.error('Error:', err);
      }
    });
  }
}