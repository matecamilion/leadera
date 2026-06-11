import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  TareaService,
  TareaDTO,
  AsistenteStats,
} from '../../core/services/tarea-service';
import { NotificationService } from '../../core/services/notification-service';

type SeccionMiEquipo = 'asistentes' | 'tareas';
type TipoTarea = 'discreta' | 'cuota';

// Vista "Mi equipo" del AGENTE (o DUENO con asistentes): gestiona sus asistentes
// y les crea tareas del día. Distinta de pages/equipo (la del dueño por rol):
// acá la autoridad es la relación de supervisión, validada en el backend.
@Component({
  selector: 'app-mi-equipo-agente',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mi-equipo-agente.html',
  styleUrl: './mi-equipo-agente.css',
})
export class MiEquipoAgente implements OnInit {
  private tareaService = inject(TareaService);
  private notificaciones = inject(NotificationService);
  private fb = inject(FormBuilder);

  seccion = signal<SeccionMiEquipo>('asistentes');

  // --- Asistentes ---
  asistentes = signal<AsistenteStats[]>([]);
  cargandoAsistentes = signal(true);
  errorAsistentes = signal<string>('');
  guardandoAsistente = signal(false);
  errorModal = signal<string>('');
  cambiandoActivoId = signal<number | null>(null);

  // --- Tareas ---
  asistenteSeleccionadoId = signal<number | null>(null);
  tareas = signal<TareaDTO[]>([]);
  cargandoTareas = signal(false);
  errorTareas = signal<string>('');
  guardandoTarea = signal(false);
  tipoTarea = signal<TipoTarea>('discreta');

  cuotas = computed(() => this.tareas().filter(t => t.objetivo != null));
  discretas = computed(() => this.tareas().filter(t => t.objetivo == null));

  asistenteSeleccionado = computed(() =>
    this.asistentes().find(a => a.id === this.asistenteSeleccionadoId()) ?? null
  );

  // Espejo de CrearAgenteEquipoRequest (mismas reglas que el modal del dueño).
  formAsistente: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    passwordTemporal: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
  });

  // Espejo de CrearTareaRequest (objetivo solo aplica si tipo = cuota).
  formTarea: FormGroup = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(200)]],
    objetivo: [3, [Validators.min(1)]],
    fechaObjetivo: [this.hoyISO(), [Validators.required]],
  });

  ngOnInit(): void {
    this.cargarAsistentes();
  }

  private hoyISO(): string {
    const hoy = new Date();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${hoy.getFullYear()}-${mes}-${dia}`;
  }

  cambiarSeccion(seccion: SeccionMiEquipo) {
    this.seccion.set(seccion);
    if (seccion === 'tareas' && this.asistenteSeleccionadoId() === null && this.asistentes().length > 0) {
      this.seleccionarAsistente(this.asistentes()[0].id);
    }
  }

  // Click en una fila de la tabla: salta al tab Tareas con ese asistente.
  verTareasDe(asistente: AsistenteStats) {
    this.seccion.set('tareas');
    this.seleccionarAsistente(asistente.id);
  }

  // ---------- Asistentes ----------

  cargarAsistentes() {
    this.cargandoAsistentes.set(true);
    this.errorAsistentes.set('');
    this.tareaService.listarAsistentes().subscribe({
      next: (lista) => {
        this.asistentes.set(lista);
        this.cargandoAsistentes.set(false);
      },
      error: () => {
        this.errorAsistentes.set('No se pudieron cargar tus asistentes.');
        this.cargandoAsistentes.set(false);
      },
    });
  }

  abrirModalAsistente(modal: HTMLDialogElement) {
    this.formAsistente.reset({ nombre: '', apellido: '', email: '', passwordTemporal: '' });
    this.errorModal.set('');
    modal.showModal();
  }

  generarPasswordAleatoria() {
    // Legible para dictar por teléfono: sin caracteres ambiguos (0/O, 1/l).
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    let password = '';
    const random = new Uint32Array(10);
    crypto.getRandomValues(random);
    for (const valor of random) {
      password += chars[valor % chars.length];
    }
    this.formAsistente.patchValue({ passwordTemporal: password });
  }

  crearAsistente(modal: HTMLDialogElement) {
    if (this.formAsistente.invalid) {
      this.formAsistente.markAllAsTouched();
      return;
    }
    this.guardandoAsistente.set(true);
    this.errorModal.set('');
    this.tareaService.crearAsistente(this.formAsistente.value).subscribe({
      next: (nuevo) => {
        this.guardandoAsistente.set(false);
        modal.close();
        // El alta devuelve AgenteEquipoDTO (sin tareasCompletadasHoy y con
        // leadsActivos=0): se completa con los datos conocidos para no recargar.
        const leadsActivos = this.asistentes()[0]?.leadsActivos ?? 0;
        this.asistentes.update(lista => [...lista, {
          id: nuevo.id,
          nombre: nuevo.nombre,
          apellido: nuevo.apellido,
          email: nuevo.email,
          activo: nuevo.activo,
          leadsActivos,
          tareasCompletadasHoy: 0,
        }]);
        this.notificaciones.exito(
          `Asistente ${nuevo.nombre} ${nuevo.apellido} creado. Pasale la contraseña temporal: la va a cambiar en su primer ingreso.`
        );
      },
      error: (err) => {
        this.guardandoAsistente.set(false);
        this.errorModal.set(err?.error?.message || 'No se pudo crear el asistente.');
      },
    });
  }

  toggleActivo(asistente: AsistenteStats) {
    const accion = asistente.activo ? 'desactivar' : 'activar';
    const detalle = asistente.activo
      ? 'No va a poder ingresar hasta que lo reactives.'
      : 'Va a volver a poder ingresar con su cuenta.';
    if (!confirm(`¿Seguro que querés ${accion} a ${asistente.nombre} ${asistente.apellido}? ${detalle}`)) {
      return;
    }

    this.cambiandoActivoId.set(asistente.id);
    this.tareaService.cambiarActivoAsistente(asistente.id, !asistente.activo).subscribe({
      next: (actualizado) => {
        this.cambiandoActivoId.set(null);
        this.asistentes.update(lista => lista.map(a => a.id === actualizado.id ? actualizado : a));
        this.notificaciones.exito(
          `${actualizado.nombre} ${actualizado.apellido} quedó ${actualizado.activo ? 'activo' : 'inactivo'}.`
        );
      },
      error: (err) => {
        this.cambiandoActivoId.set(null);
        this.notificaciones.error(err?.error?.message || 'No se pudo cambiar el estado del asistente.');
      },
    });
  }

  esInvalido(campo: string): boolean {
    const c = this.formAsistente.get(campo);
    return !!(c && c.invalid && c.touched);
  }

  // ---------- Tareas ----------

  seleccionarAsistente(id: number) {
    this.asistenteSeleccionadoId.set(id);
    this.cargarTareas(id);
  }

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

  cambiarTipoTarea(tipo: TipoTarea) {
    this.tipoTarea.set(tipo);
  }

  crearTarea() {
    const asistenteId = this.asistenteSeleccionadoId();
    if (asistenteId === null) return;
    if (this.formTarea.invalid) {
      this.formTarea.markAllAsTouched();
      return;
    }
    const valores = this.formTarea.value;
    const esCuota = this.tipoTarea() === 'cuota';

    this.guardandoTarea.set(true);
    this.tareaService.crearTarea({
      titulo: valores.titulo,
      asignadoAId: asistenteId,
      objetivo: esCuota ? valores.objetivo : null,
      // El backend espera LocalDateTime; fin del día para que la tarea valga
      // toda la jornada elegida.
      fechaObjetivo: `${valores.fechaObjetivo}T23:59:59`,
    }).subscribe({
      next: (creada) => {
        this.guardandoTarea.set(false);
        this.tareas.update(lista => [...lista, creada]);
        this.formTarea.reset({ titulo: '', objetivo: 3, fechaObjetivo: this.hoyISO() });
        this.notificaciones.exito('Tarea creada.');
      },
      error: (err) => {
        this.guardandoTarea.set(false);
        this.notificaciones.error(err?.error?.message || 'No se pudo crear la tarea.');
      },
    });
  }

  esInvalidoTarea(campo: string): boolean {
    const c = this.formTarea.get(campo);
    return !!(c && c.invalid && c.touched);
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
