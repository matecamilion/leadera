import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OperacionService, EstadoOperacion } from '../../core/services/operacion-service';
import { OperacionPipeline } from '../../core/models/operacion-pipeline';

@Component({
  selector: 'app-operaciones-cerradas',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './operaciones-cerradas.html',
  styleUrl: './operaciones-cerradas.css',
})
export class OperacionesCerradasComponent implements OnInit {
  private operacionService = inject(OperacionService);

  operaciones = signal<OperacionPipeline[]>([]);
  cargando = signal<boolean>(true);
  error = signal<string>('');

  filtroEstado = signal<string>('');

  readonly estados: EstadoOperacion[] = ['CERRADA_GANADA', 'CANCELADA'];

  operacionesFiltradas = computed(() => {
    const estado = this.filtroEstado();
    if (!estado) return this.operaciones();
    return this.operaciones().filter((o) => o.estado === estado);
  });

  totalGanadas = computed(() =>
    this.operaciones().filter((o) => o.estado === 'CERRADA_GANADA').length
  );

  totalCanceladas = computed(() =>
    this.operaciones().filter((o) => o.estado === 'CANCELADA').length
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.operacionService.obtenerOperacionesCerradas().subscribe({
      next: (data) => {
        this.operaciones.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar operaciones cerradas', err);
        this.error.set('No se pudieron cargar las operaciones cerradas.');
        this.cargando.set(false);
      },
    });
  }

  onEstadoChange(valor: string): void {
    this.filtroEstado.set(valor);
  }

  formatearEstado(estado: string): string {
    return estado.replace('_', ' ');
  }

  trackById(_: number, op: OperacionPipeline): number {
    return op.id;
  }
}
