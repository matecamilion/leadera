import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OperacionService, Operacion } from '../../core/services/operacion-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';
import { EventoOperacion } from '../../core/models/evento-operacion';

@Component({
  selector: 'app-detalle-operacion',
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './detalle-operacion.html',
  styleUrl: './detalle-operacion.css',
})
export class DetalleOperacion implements OnInit {
  private route = inject(ActivatedRoute);
  private operacionService = inject(OperacionService);
  private leadService = inject(LeadService);

  operacion = signal<Operacion | null>(null);
  leadAsociado = signal<Lead | null>(null);

  leadId!: number;
  operacionId!: number;
  guardandoEstado = signal<boolean>(false);
  errorEstado = signal<string>('');

  eventos = signal<EventoOperacion[]>([]);
  nuevoEvento: Partial<EventoOperacion> = {};
  mostrarFormEvento = signal<boolean>(false);
  guardandoEvento = signal<boolean>(false);
  errorEvento = signal<string>('');

  estadosOperacion = [
    'PUBLICADA',
    'RESERVADA',
    'EN_NEGOCIACION',
    'CERRADA_GANADA',
    'CANCELADA'
  ];

  readonly STEP_ORDER = ['PUBLICADA', 'RESERVADA', 'EN_NEGOCIACION', 'CERRADA_GANADA'];

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
      next: (data) => {
        this.operacion.set(data);
        this.leadService.getLeadById(this.leadId).subscribe({
          next: (lead) => this.leadAsociado.set(lead),
          error: () => {}
        });
      },
      error: (err) => console.error('Error al cargar operación', err)
    });
    this.cargarEventos();
  }

  cargarEventos() {
    this.operacionService.obtenerEventos(this.leadId, this.operacionId).subscribe({
      next: (data) => this.eventos.set(data),
      error: (err) => console.error('Error al cargar eventos', err)
    });
  }

  toggleFormEvento() {
    this.mostrarFormEvento.update(v => !v);
    this.errorEvento.set('');
  }

  registrarEvento() {
    if (!this.nuevoEvento.tipo || !this.nuevoEvento.detalle?.trim()) {
      this.errorEvento.set('Completá tipo y detalle.');
      return;
    }

    this.guardandoEvento.set(true);
    this.errorEvento.set('');

    this.operacionService
      .registrarEvento(this.leadId, this.operacionId, this.nuevoEvento)
      .subscribe({
        next: (creado) => {
          this.eventos.update(list => [creado, ...list]);
          this.nuevoEvento = {};
          this.mostrarFormEvento.set(false);
          this.guardandoEvento.set(false);
        },
        error: (err) => {
          console.error('Error al registrar evento', err);
          this.errorEvento.set(err?.error?.message ?? 'No se pudo registrar el evento.');
          this.guardandoEvento.set(false);
        }
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

  isPastStep(paso: string, actual: string): boolean {
    const iActual = this.STEP_ORDER.indexOf(actual);
    const iPaso = this.STEP_ORDER.indexOf(paso);
    return iPaso >= 0 && iActual > iPaso;
  }

  stepClass(paso: string, actual: string): string {
    if (actual === paso) return 'active';
    if (this.isPastStep(paso, actual)) return 'done';
    return '';
  }

  iniciales(lead: Lead | null): string {
    if (!lead) return '?';
    return `${lead.nombre?.charAt(0) ?? ''}${lead.apellido?.charAt(0) ?? ''}`.toUpperCase();
  }
}
