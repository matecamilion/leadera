import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { OperacionService, Operacion } from '../../core/services/operacion-service';


@Component({
  selector: 'app-detalle-operacion',
  imports: [CommonModule, RouterModule],
  templateUrl: './detalle-operacion.html',
  styleUrl: './detalle-operacion.css',
})
export class DetalleOperacion implements OnInit {
   private route = inject(ActivatedRoute);
  private operacionService = inject(OperacionService);

  operacion = signal<Operacion | null>(null);

  leadId!: number;
  operacionId!: number;
  guardandoEstado = signal<boolean>(false);
errorEstado = signal<string>('');

estadosOperacion = [
  'ABIERTA',
  'EN_GESTION',
  'RESERVADA',
  'CERRADA_GANADA',
  'CANCELADA'
];

  ngOnInit(): void {
    const leadIdParam = this.route.snapshot.paramMap.get('leadId');
    const operacionIdParam = this.route.snapshot.paramMap.get('operacionId');

    if (leadIdParam && operacionIdParam) {
      this.leadId = Number(leadIdParam);
      this.operacionId = Number(operacionIdParam);

      this.cargarOperacion();
    }
  }

  cargarOperacion() {
    this.operacionService.obtenerOperacionPorId(this.leadId, this.operacionId).subscribe({
      next: (data) => this.operacion.set(data),
      error: (err) => console.error('Error al cargar operación', err)
    });
  }

  cambiarEstadoOperacion(nuevoEstado: string) {
  const operacionActual = this.operacion();

  if (!operacionActual) return;

  if (operacionActual.estadoOperacion === nuevoEstado) return;

  this.guardandoEstado.set(true);
  this.errorEstado.set('');

  this.operacionService.cambiarEstadoOperacion(
    this.leadId,
    this.operacionId,
    nuevoEstado
  ).subscribe({
    next: (operacionActualizada) => {
      this.operacion.set(operacionActualizada);
      this.guardandoEstado.set(false);
    },
    error: (err) => {
      console.error('Error al cambiar estado de operación', err);
      this.errorEstado.set('No se pudo cambiar el estado de la operación.');
      this.guardandoEstado.set(false);
    }
  });
}

formatearEstado(estado: string): string {
  return estado.replace('_', ' ');
}
}
