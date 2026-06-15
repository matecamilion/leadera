import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { LeadService } from '../../core/services/lead-service';
import { LeadsHoyResponse } from '../../core/models/leads-hoy-response';
import { LeadSection } from '../../shared/lead-section/lead-section';
import { RouterModule } from '@angular/router';
import { MatchingService, MatchDelDia } from '../../core/services/matching-service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [LeadSection, RouterModule, NgClass],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private matchingService = inject(MatchingService);

  cargando = signal(true);
  error = signal('');
  leadsHoy = signal<LeadsHoyResponse | null>(null);
  matches = signal<MatchDelDia[]>([]);

  totalCalientes = computed(() => this.leadsHoy()?.prioritarios?.length ?? 0);
  totalNuevos    = computed(() => this.leadsHoy()?.nuevosSinContacto?.length ?? 0);
  totalSeguim    = computed(() => this.leadsHoy()?.seguimientosDeHoy?.length ?? 0);

  totalOriginal = computed(() => this.leadsHoy()?.totalTareasDelDia ?? 0);
  completadas = computed(() => this.leadsHoy()?.tareasCompletadasDelDia ?? 0);
  totalMatches = computed(() => this.matches().length);

  totalPendientes = computed(() => {
    const data = this.leadsHoy();
    if (!data) return 0;
    return (
      (data.nuevosSinContacto?.length ?? 0) +
      (data.prioritarios?.length ?? 0) +
      (data.seguimientosDeHoy?.length ?? 0)
    );
  });

  progreso = computed(() => {
    const t = this.totalOriginal();
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
      },
    });

    this.matchingService.obtenerMisMatches().subscribe({
      next: (data) => this.matches.set(data),
      error: () => {},
    });
  }
}
