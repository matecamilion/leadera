import { Component, OnInit, signal, computed } from '@angular/core';
import { LeadService } from '../../core/services/lead-service';
import { LeadsHoyResponse } from '../../core/models/leads-hoy-response';
import { LeadSection } from '../../shared/lead-section/lead-section';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [LeadSection, RouterModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  cargando = signal(true);
  error = signal('');
  leadsHoy = signal<LeadsHoyResponse | null>(null);

  // Computeds basadas en la respuesta del server
  total = computed(() => this.leadsHoy()?.totalTareasDelDia ?? 0);
  completadas = computed(() => this.leadsHoy()?.tareasCompletadasDelDia ?? 0);

  progreso = computed(() => {
    const t = this.total();
    return t > 0 ? Math.round((this.completadas() / t) * 100) : 0;
  });

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.leadService.getLeadsHoy().subscribe({
      next: (data) => {
        this.leadsHoy.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Error al conectar con el servidor');
        this.cargando.set(false);
      }
    });
  }

  // Se dispara cuando contactás a alguien desde LeadSection
  actualizarVista(leadId: number): void {
    this.cargarDatos(); 
  }
}