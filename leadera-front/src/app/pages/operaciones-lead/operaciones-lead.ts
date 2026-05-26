import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import {
  OperacionService,
  Operacion,
  EstadoOperacion,
  TipoOperacion,
} from '../../core/services/operacion-service';

@Component({
  selector: 'app-operaciones-lead',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './operaciones-lead.html',
  styleUrl: './operaciones-lead.css',
})
export class OperacionesLead implements OnInit {
  private route = inject(ActivatedRoute);
  private leadService = inject(LeadService);
  private operacionService = inject(OperacionService);

  leadId = 0;
  lead = signal<any>(null);
  operaciones = signal<Operacion[]>([]);
  cargando = signal<boolean>(true);
  error = signal<string>('');

  filtroEstado = signal<string>('');
  filtroTipo = signal<string>('');

  readonly estados: EstadoOperacion[] = [
    'ABIERTA',
    'PUBLICADA',
    'RESERVADA',
    'EN_NEGOCIACION',
    'CERRADA_GANADA',
    'CANCELADA',
  ];
  readonly tipos: TipoOperacion[] = ['VENTA', 'COMPRA', 'ALQUILER'];

  operacionesFiltradas = computed(() => {
    const estado = this.filtroEstado();
    const tipo = this.filtroTipo();
    return this.operaciones().filter((o) => {
      const coincideEstado = !estado || o.estadoOperacion === estado;
      const coincideTipo = !tipo || o.tipoOperacion === tipo;
      return coincideEstado && coincideTipo;
    });
  });

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

    this.operacionService.obtenerOperacionesDelLead(this.leadId).subscribe({
      next: (data) => {
        this.operaciones.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar operaciones', err);
        this.error.set('No se pudieron cargar las operaciones.');
        this.cargando.set(false);
      },
    });
  }

  onEstadoChange(valor: string): void {
    this.filtroEstado.set(valor);
  }

  onTipoChange(valor: string): void {
    this.filtroTipo.set(valor);
  }

  formatearEstado(estado: string): string {
    return estado.replace('_', ' ');
  }

  trackById(_: number, op: Operacion): number {
    return op.id;
  }
}
