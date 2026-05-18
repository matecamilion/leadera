import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { OperacionService, EstadoOperacion } from '../../core/services/operacion-service';
import { OperacionPipeline } from '../../core/models/operacion-pipeline';

@Component({
  selector: 'app-operaciones-por-estado',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe],
  templateUrl: './operaciones-por-estado.html',
  styleUrl: './operaciones-por-estado.css',
})
export class OperacionesPorEstado {
  private route = inject(ActivatedRoute);
  private operacionService = inject(OperacionService);

  estado = signal<EstadoOperacion>('ABIERTA');
  operaciones = signal<OperacionPipeline[]>([]);
  cargando = signal<boolean>(true);
  error = signal<string>('');

  estadoLabel = computed(() => this.estado().replace('_', ' '));

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const raw = (params.get('estado') || 'ABIERTA').toUpperCase() as EstadoOperacion;
      this.estado.set(raw);
      this.cargar();
    });
  }

  private cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.operacionService.obtenerTodasLasOperaciones().subscribe({
      next: data => {
        this.operaciones.set(data.filter(o => o.estado === this.estado()));
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las operaciones.');
        this.cargando.set(false);
      },
    });
  }

  tipoClass(tipo: string): string {
    return `badge-tipo tipo-${tipo.toLowerCase()}`;
  }
}
