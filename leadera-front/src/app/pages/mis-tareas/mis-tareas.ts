import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  TareaService,
  TareaDTO,
  TareasCumplimientoDTO,
  AsistenteStats,
} from '../../core/services/tarea-service';
import { AuthService } from '../../core/services/auth-service';
import { NotificationService } from '../../core/services/notification-service';

type TipoTarea = 'discreta' | 'cuota';

// "Mis tareas de hoy": el actor (agente o asistente) ve y gestiona sus tareas
// del día. Se muestran solo las pendientes al cargar; una discreta tildada
// queda tachada hasta el próximo refresh. AGENTE/DUENO además pueden crear
// tareas (para sí mismos o para un asistente propio).
@Component({
  selector: 'app-mis-tareas',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mis-tareas.html',
  styleUrl: './mis-tareas.css',
})
export class MisTareas implements OnInit {
  private tareaService = inject(TareaService);
  private authService = inject(AuthService);
  private notificaciones = inject(NotificationService);
  private fb = inject(FormBuilder);

  tareas = signal<TareaDTO[]>([]);
  cargando = signal(true);
  // Ids con completado en vuelo (evita doble click durante el PATCH).
  completandoIds = signal<Set<number>>(new Set());

  // --- Modal nueva tarea (solo AGENTE/DUENO) ---
  asistentes = signal<AsistenteStats[]>([]);
  guardandoTarea = signal(false);
  errorModal = signal<string>('');
  tipoTarea = signal<TipoTarea>('discreta');

  historial = signal<TareaDTO[]>([]);
  cargandoHistorial = signal(false);
  historialAbierto = signal(false);
  cumplimiento = signal<TareasCumplimientoDTO | null>(null);

  cuotas = computed(() => this.tareas().filter(t => t.objetivo != null));
  discretas = computed(() => this.tareas().filter(t => t.objetivo == null));
  vencidas = computed(() => this.tareas().filter(t => t.diasVencida != null && t.diasVencida > 0));
  hoy = computed(() => this.tareas().filter(t => t.diasVencida == null || t.diasVencida === 0));

  // 'yo' = autoasignación; un id numérico = asistente.
  formTarea: FormGroup = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(200)]],
    asignadoA: ['yo', [Validators.required]],
    objetivo: [3, [Validators.min(1)]],
    fechaObjetivo: [this.hoyISO(), [Validators.required]],
  });

  get esAsistente(): boolean {
    return this.authService.esAsistente();
  }

  ngOnInit(): void {
    this.cargarTareas();
    this.cargarCumplimiento();
    if (!this.esAsistente) {
      this.tareaService.listarAsistentes().subscribe({
        next: (lista) => this.asistentes.set(lista),
        error: () => this.asistentes.set([]),
      });
    }
  }

  // "Miércoles 11 de junio" — armado a mano para no depender del locale registrado.
  fechaDeHoy(): string {
    const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    const meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
                   'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    const hoy = new Date();
    return `${dias[hoy.getDay()]} ${hoy.getDate()} de ${meses[hoy.getMonth()]}`;
  }

  private hoyISO(): string {
    const hoy = new Date();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${hoy.getFullYear()}-${mes}-${dia}`;
  }

  cargarTareas(): void {
    this.cargando.set(true);
    this.tareaService.obtenerTareasDelDia().subscribe({
      next: (tareas) => {
        // Solo las pendientes: las completadas no interesan en esta vista.
        this.tareas.set(tareas.filter(t => !t.completada));
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.notificaciones.error('No se pudieron cargar tus tareas.');
      },
    });
  }

  // Tilda una discreta con optimistic update: marca al instante, revierte con
  // toast si el backend falla. Las cuotas no se tocan (se completan solas).
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
      error: (err) => {
        this.tareas.update(lista =>
          lista.map(t => t.id === tarea.id ? { ...t, completada: false } : t)
        );
        this.quitarDeEnVuelo(tarea.id);
        this.notificaciones.error(err?.error?.message || 'No se pudo completar la tarea.');
      },
    });
  }

  private quitarDeEnVuelo(id: number): void {
    this.completandoIds.update(set => {
      const nuevo = new Set(set);
      nuevo.delete(id);
      return nuevo;
    });
  }

  // ---------- Modal nueva tarea ----------

  abrirModal(modal: HTMLDialogElement) {
    this.formTarea.reset({ titulo: '', asignadoA: 'yo', objetivo: 3, fechaObjetivo: this.hoyISO() });
    this.tipoTarea.set('discreta');
    this.errorModal.set('');
    modal.showModal();
  }

  cambiarTipoTarea(tipo: TipoTarea) {
    this.tipoTarea.set(tipo);
  }

  crearTarea(modal: HTMLDialogElement) {
    if (this.formTarea.invalid) {
      this.formTarea.markAllAsTouched();
      return;
    }
    const valores = this.formTarea.value;
    const esCuota = this.tipoTarea() === 'cuota';
    const esParaMi = valores.asignadoA === 'yo';
    const asignadoAId = esParaMi
      ? this.authService.getIdAgente()
      : Number(valores.asignadoA);

    if (asignadoAId == null) {
      this.errorModal.set('No se pudo resolver tu usuario. Volvé a iniciar sesión.');
      return;
    }

    this.guardandoTarea.set(true);
    this.errorModal.set('');
    this.tareaService.crearTarea({
      titulo: valores.titulo,
      asignadoAId,
      objetivo: esCuota ? valores.objetivo : null,
      // El backend espera LocalDateTime; fin del día para que valga toda la jornada.
      fechaObjetivo: `${valores.fechaObjetivo}T23:59:59`,
    }).subscribe({
      next: () => {
        this.guardandoTarea.set(false);
        modal.close();
        if (esParaMi) {
          // Recarga para que aparezca en la lista (y con el progreso del backend).
          this.cargarTareas();
          this.notificaciones.exito('Tarea creada.');
        } else {
          const asistente = this.asistentes().find(a => a.id === asignadoAId);
          this.notificaciones.exito(
            `Tarea asignada a ${asistente ? asistente.nombre + ' ' + asistente.apellido : 'tu asistente'}.`
          );
        }
      },
      error: (err) => {
        this.guardandoTarea.set(false);
        this.errorModal.set(err?.error?.message || 'No se pudo crear la tarea.');
      },
    });
  }

  esInvalido(campo: string): boolean {
    const c = this.formTarea.get(campo);
    return !!(c && c.invalid && c.touched);
  }

  cargarCumplimiento(): void {
    this.tareaService.obtenerCumplimientoMes().subscribe({
      next: (c) => this.cumplimiento.set(c),
      error: () => {},
    });
  }

  toggleHistorial(): void {
    if (!this.historialAbierto() && this.historial().length === 0) {
      this.cargandoHistorial.set(true);
      this.tareaService.obtenerHistorial().subscribe({
        next: (h) => { this.historial.set(h); this.cargandoHistorial.set(false); },
        error: () => this.cargandoHistorial.set(false),
      });
    }
    this.historialAbierto.update(v => !v);
  }

  etiquetaVencida(t: TareaDTO): string {
    if (!t.diasVencida) return '';
    return t.diasVencida === 1 ? 'Vencida hace 1 día' : `Vencida hace ${t.diasVencida} días`;
  }

  // ---------- Helpers visuales (cuotas) ----------

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
