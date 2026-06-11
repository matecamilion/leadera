import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  TareaService,
  TareaDTO,
  AsistenteResumen,
} from '../../core/services/tarea-service';

// Vista "Mi Asistente" del AGENTE/DUENO: ve las tareas del día de su asistente,
// el progreso de las cuotas y el checklist de tareas discretas. Solo lectura:
// completar una tarea discreta es acción del asistente, no del supervisor.
@Component({
  selector: 'app-mi-asistente',
  imports: [CommonModule],
  templateUrl: './mi-asistente.html',
  styleUrl: './mi-asistente.css',
})
export class MiAsistente implements OnInit {
  private tareaService = inject(TareaService);

  asistentes = signal<AsistenteResumen[]>([]);
  asistenteSeleccionadoId = signal<number | null>(null);
  tareas = signal<TareaDTO[]>([]);

  cargandoAsistentes = signal(true);
  cargandoTareas = signal(false);
  errorAsistentes = signal<string>('');
  errorTareas = signal<string>('');

  // Cuotas: objetivo != null → se calcula progreso. Discretas: objetivo == null.
  cuotas = computed(() => this.tareas().filter(t => t.objetivo != null));
  discretas = computed(() => this.tareas().filter(t => t.objetivo == null));

  asistenteSeleccionado = computed(() =>
    this.asistentes().find(a => a.id === this.asistenteSeleccionadoId()) ?? null
  );

  ngOnInit(): void {
    this.cargandoAsistentes.set(true);
    this.errorAsistentes.set('');
    this.tareaService.obtenerMisAsistentes().subscribe({
      next: (lista) => {
        this.asistentes.set(lista);
        this.cargandoAsistentes.set(false);
        // Con uno o más, se selecciona el primero y se cargan sus tareas.
        if (lista.length > 0) {
          this.seleccionarAsistente(lista[0].id);
        }
      },
      error: () => {
        this.errorAsistentes.set('No se pudieron cargar tus asistentes.');
        this.cargandoAsistentes.set(false);
      },
    });
  }

  seleccionarAsistente(id: number) {
    this.asistenteSeleccionadoId.set(id);
    this.cargarTareas(id);
  }

  // Soporta el <select> del template (recibe el value como string).
  onSeleccionarAsistente(valor: string) {
    this.seleccionarAsistente(Number(valor));
  }

  private cargarTareas(asistenteId: number) {
    this.cargandoTareas.set(true);
    this.errorTareas.set('');
    this.tareas.set([]);
    this.tareaService.obtenerTareasDeAsistente(asistenteId).subscribe({
      next: (tareas) => {
        this.tareas.set(tareas);
        this.cargandoTareas.set(false);
      },
      error: () => {
        this.errorTareas.set('No se pudieron cargar las tareas del asistente.');
        this.cargandoTareas.set(false);
      },
    });
  }

  // % de avance de una cuota, acotado a 100.
  porcentaje(tarea: TareaDTO): number {
    if (!tarea.objetivo || tarea.objetivo <= 0) return 0;
    return Math.min(100, Math.round((tarea.progreso / tarea.objetivo) * 100));
  }

  // Clave de estado para el badge: completada / en-progreso / pendiente.
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
