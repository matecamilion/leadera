import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { LeadHoy } from '../../core/models/lead-hoy';
import { LeadSection } from '../../shared/lead-section/lead-section';
import { AuthService } from '../../core/services/auth-service';
import { TareaService, TareaDTO } from '../../core/services/tarea-service';

@Component({
  selector: 'app-gestion-del-dia',
  standalone: true,
  imports: [CommonModule, RouterModule, LeadSection],
  templateUrl: './gestion-del-dia.html',
  styleUrl: './gestion-del-dia.css'
})
export class GestionDelDia implements OnInit {
  leads = signal<LeadHoy[]>([]);
  cargando = signal(true);

  // --- Tareas del día (solo para rol ASISTENTE) ---
  tareas = signal<TareaDTO[]>([]);
  cargandoTareas = signal(false);
  // Ids con completado en vuelo (evita doble click durante el PATCH).
  completandoIds = signal<Set<number>>(new Set());

  cuotas = computed(() => this.tareas().filter(t => t.objetivo != null));
  discretas = computed(() => this.tareas().filter(t => t.objetivo == null));

  constructor(
    private leadService: LeadService,
    private authService: AuthService,
    private tareaService: TareaService,
  ) {}

  get esAsistente(): boolean {
    return this.authService.esAsistente();
  }

  ngOnInit(): void {
    this.cargarHistorial();
    // Solo el asistente tiene tareas asignadas: el resto ni llama al backend.
    if (this.esAsistente) {
      this.cargarTareas();
    }
  }

  cargarHistorial(): void {
    this.cargando.set(true);
    this.leadService.getLeadsHoy().subscribe({
      next: (res) => {
        // Solo nos interesan los que ya fueron contactados
        this.leads.set(res.contactadosHoy || []);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  cargarTareas(): void {
    this.cargandoTareas.set(true);
    this.tareaService.obtenerTareasDelDia().subscribe({
      next: (tareas) => {
        this.tareas.set(tareas);
        this.cargandoTareas.set(false);
      },
      error: () => {
        this.cargandoTareas.set(false);
      }
    });
  }

  // Tilda una tarea discreta con optimistic update: se marca al instante y se
  // revierte si el backend falla. Las cuotas no se tocan (se completan solas).
  completarTarea(tarea: TareaDTO): void {
    if (tarea.completada || tarea.objetivo != null) return;
    if (this.completandoIds().has(tarea.id)) return;

    this.completandoIds.update(set => new Set(set).add(tarea.id));
    this.tareas.update(lista =>
      lista.map(t => t.id === tarea.id ? { ...t, completada: true } : t)
    );

    this.tareaService.completarTarea(tarea.id).subscribe({
      next: (actualizada) => {
        this.tareas.update(lista =>
          lista.map(t => t.id === actualizada.id ? actualizada : t)
        );
        this.quitarDeEnVuelo(tarea.id);
      },
      error: () => {
        // Revertir el tilde optimista.
        this.tareas.update(lista =>
          lista.map(t => t.id === tarea.id ? { ...t, completada: false } : t)
        );
        this.quitarDeEnVuelo(tarea.id);
      }
    });
  }

  private quitarDeEnVuelo(id: number): void {
    this.completandoIds.update(set => {
      const nuevo = new Set(set);
      nuevo.delete(id);
      return nuevo;
    });
  }

  // ---- Helpers visuales de cuotas ----

  porcentaje(tarea: TareaDTO): number {
    if (!tarea.objetivo || tarea.objetivo <= 0) return 0;
    return Math.min(100, Math.round((tarea.progreso / tarea.objetivo) * 100));
  }

  estadoCuota(tarea: TareaDTO): 'completada' | 'en-progreso' | 'pendiente' {
    if (tarea.completada) return 'completada';
    if (tarea.progreso > 0) return 'en-progreso';
    return 'pendiente';
  }

  etiquetaEstado(tarea: TareaDTO): string {
    switch (this.estadoCuota(tarea)) {
      case 'completada': return 'Completada';
      case 'en-progreso': return 'En progreso';
      default: return 'Pendiente';
    }
  }
}
