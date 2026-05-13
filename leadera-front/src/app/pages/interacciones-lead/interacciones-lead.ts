import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { InteraccionService } from '../../core/services/interaccion-service';
import { Interaccion } from '../../core/models/interaccion';

@Component({
  selector: 'app-interacciones-lead',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './interacciones-lead.html',
  styleUrl: './interacciones-lead.css',
})
export class InteraccionesLead implements OnInit {
  private route = inject(ActivatedRoute);
  private leadService = inject(LeadService);
  private interaccionService = inject(InteraccionService);

  leadId = 0;
  lead = signal<any>(null);
  interacciones = signal<Interaccion[]>([]);
  cargando = signal<boolean>(true);
  error = signal<string>('');

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.leadId = Number(idParam);
      this.cargar();
    }
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');

    this.leadService.getLeadById(this.leadId).subscribe({
      next: (data) => this.lead.set(data),
      error: (err) => console.error('Error al cargar lead', err),
    });

    this.interaccionService.obtenerInteraccionesDelLead(this.leadId).subscribe({
      next: (data) => {
        const ordenadas = [...data].sort((a, b) => {
          const fechaA = a.fechaInteraccion ?? '';
          const fechaB = b.fechaInteraccion ?? '';
          return fechaB.localeCompare(fechaA);
        });
        this.interacciones.set(ordenadas);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar interacciones', err);
        this.error.set('No se pudieron cargar las interacciones.');
        this.cargando.set(false);
      },
    });
  }

  trackById(_: number, i: Interaccion): number {
    return i.id ?? 0;
  }
}
