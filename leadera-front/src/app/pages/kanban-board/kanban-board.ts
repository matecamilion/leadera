import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  DragDropModule,
  CdkDragDrop,
  transferArrayItem,
} from '@angular/cdk/drag-drop';
import { OperacionService, EstadoOperacion } from '../../core/services/operacion-service';
import {
  OperacionPipeline,
  ESTADOS_PIPELINE,
} from '../../core/models/operacion-pipeline';

interface Columna {
  estado: EstadoOperacion;
  operaciones: OperacionPipeline[];
}

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [CommonModule, RouterModule, DragDropModule],
  templateUrl: './kanban-board.html',
  styleUrl: './kanban-board.css',
})
export class KanbanBoardComponent implements OnInit {
  private operacionService = inject(OperacionService);

  cargando = signal<boolean>(true);
  error = signal<string>('');

  readonly estados = ESTADOS_PIPELINE;

  columnas = signal<Columna[]>([]);

  ngOnInit(): void {
    this.cargarPipeline();
  }

  cargarPipeline(): void {
    this.cargando.set(true);
    this.error.set('');
    this.operacionService.obtenerTodasLasOperaciones().subscribe({
      next: (data) => {
        this.columnas.set(
          this.estados.map((estado) => ({
            estado,
            operaciones: data.filter((o) => o.estado === estado),
          }))
        );
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar pipeline', err);
        this.error.set('No se pudieron cargar las operaciones.');
        this.cargando.set(false);
      },
    });
  }

  onDrop(event: CdkDragDrop<OperacionPipeline[]>, estadoDestino: EstadoOperacion): void {
    if (event.previousContainer === event.container) {
      return;
    }

    const op = event.previousContainer.data[event.previousIndex];
    const estadoOrigen = op.estado;

    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex
    );
    op.estado = estadoDestino;
    this.columnas.update((c) => [...c]);

    this.operacionService
      .cambiarEstadoOperacion(op.leadId, op.id, estadoDestino)
      .subscribe({
        error: () => {
          transferArrayItem(
            event.container.data,
            event.previousContainer.data,
            event.currentIndex,
            event.previousIndex
          );
          op.estado = estadoOrigen;
          this.columnas.update((c) => [...c]);
        },
      });
  }

  formatearEstado(estado: string): string {
    return estado.replace('_', ' ');
  }

  trackById(_: number, op: OperacionPipeline): number {
    return op.id;
  }

  trackByEstado(_: number, columna: Columna): string {
    return columna.estado;
  }
}
