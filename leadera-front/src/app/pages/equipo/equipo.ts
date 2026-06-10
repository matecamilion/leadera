import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  EquipoService,
  AgenteEquipo,
  EquipoStats,
  LeadEquipo,
} from '../../core/services/equipo-service';
import { NotificationService } from '../../core/services/notification-service';
import { Page } from '../../core/models/page';

type SeccionEquipo = 'agentes' | 'stats' | 'leads';

// Vista "Mi equipo" del dueño: gestión de agentes, estadísticas del equipo
// y listado read-only de los leads de toda la inmobiliaria.
@Component({
  selector: 'app-equipo',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './equipo.html',
  styleUrl: './equipo.css',
})
export class Equipo implements OnInit {
  private equipoService = inject(EquipoService);
  private notificaciones = inject(NotificationService);
  private fb = inject(FormBuilder);

  seccion = signal<SeccionEquipo>('agentes');

  // --- Agentes ---
  agentes = signal<AgenteEquipo[]>([]);
  cargandoAgentes = signal(true);
  errorAgentes = signal<string>('');
  guardandoAgente = signal(false);
  errorModal = signal<string>('');
  cambiandoActivoId = signal<number | null>(null);

  // --- Stats ---
  stats = signal<EquipoStats | null>(null);
  cargandoStats = signal(false);
  errorStats = signal<string>('');

  // --- Leads del equipo ---
  leadsPage = signal<Page<LeadEquipo> | null>(null);
  cargandoLeads = signal(false);
  errorLeads = signal<string>('');
  paginaActual = signal(0);
  readonly tamanoPagina = 20;

  // Espejo de CrearAgenteEquipoRequest del backend.
  formAgente: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    passwordTemporal: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.cargarAgentes();
  }

  cambiarSeccion(seccion: SeccionEquipo) {
    this.seccion.set(seccion);
    if (seccion === 'stats' && !this.stats()) {
      this.cargarStats();
    }
    if (seccion === 'leads' && !this.leadsPage()) {
      this.cargarLeads(0);
    }
  }

  // ---------- Agentes ----------

  cargarAgentes() {
    this.cargandoAgentes.set(true);
    this.errorAgentes.set('');
    this.equipoService.listarEquipo().subscribe({
      next: (lista) => {
        this.agentes.set(lista);
        this.cargandoAgentes.set(false);
      },
      error: () => {
        this.errorAgentes.set('No se pudo cargar el equipo.');
        this.cargandoAgentes.set(false);
      },
    });
  }

  abrirModalAgente(modal: HTMLDialogElement) {
    this.formAgente.reset({ nombre: '', apellido: '', email: '', passwordTemporal: '' });
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
    this.formAgente.patchValue({ passwordTemporal: password });
  }

  crearAgente(modal: HTMLDialogElement) {
    if (this.formAgente.invalid) {
      this.formAgente.markAllAsTouched();
      return;
    }
    this.guardandoAgente.set(true);
    this.errorModal.set('');
    this.equipoService.crearAgente(this.formAgente.value).subscribe({
      next: (nuevo) => {
        this.guardandoAgente.set(false);
        modal.close();
        this.agentes.update(lista => [...lista, nuevo]);
        this.notificaciones.exito(
          `Agente ${nuevo.nombre} ${nuevo.apellido} creado. Pasale la contraseña temporal: la va a cambiar en su primer ingreso.`
        );
      },
      error: (err) => {
        this.guardandoAgente.set(false);
        this.errorModal.set(err?.error?.message || 'No se pudo crear el agente.');
      },
    });
  }

  toggleActivo(agente: AgenteEquipo) {
    const accion = agente.activo ? 'desactivar' : 'activar';
    const detalle = agente.activo
      ? 'No va a poder ingresar hasta que lo reactives.'
      : 'Va a volver a poder ingresar con su cuenta.';
    if (!confirm(`¿Seguro que querés ${accion} a ${agente.nombre} ${agente.apellido}? ${detalle}`)) {
      return;
    }

    this.cambiandoActivoId.set(agente.id);
    this.equipoService.cambiarActivo(agente.id, !agente.activo).subscribe({
      next: (actualizado) => {
        this.cambiandoActivoId.set(null);
        this.agentes.update(lista => lista.map(a => a.id === actualizado.id ? actualizado : a));
        this.notificaciones.exito(
          `${actualizado.nombre} ${actualizado.apellido} quedó ${actualizado.activo ? 'activo' : 'inactivo'}.`
        );
        // El cambio afecta las stats agregadas: se recargan en la próxima visita.
        this.stats.set(null);
      },
      error: (err) => {
        this.cambiandoActivoId.set(null);
        this.notificaciones.error(err?.error?.message || 'No se pudo cambiar el estado del agente.');
      },
    });
  }

  esInvalido(campo: string): boolean {
    const c = this.formAgente.get(campo);
    return !!(c && c.invalid && c.touched);
  }

  // ---------- Stats ----------

  cargarStats() {
    this.cargandoStats.set(true);
    this.errorStats.set('');
    this.equipoService.getStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.cargandoStats.set(false);
      },
      error: () => {
        this.errorStats.set('No se pudieron cargar las estadísticas del equipo.');
        this.cargandoStats.set(false);
      },
    });
  }

  // ---------- Leads del equipo ----------

  cargarLeads(pagina: number) {
    this.cargandoLeads.set(true);
    this.errorLeads.set('');
    this.paginaActual.set(pagina);
    this.equipoService.getLeadsDelEquipo(pagina, this.tamanoPagina).subscribe({
      next: (page) => {
        this.leadsPage.set(page);
        this.cargandoLeads.set(false);
      },
      error: () => {
        this.errorLeads.set('No se pudieron cargar los leads del equipo.');
        this.cargandoLeads.set(false);
      },
    });
  }

  paginaAnterior() {
    const page = this.leadsPage();
    if (page && !page.first) this.cargarLeads(this.paginaActual() - 1);
  }

  paginaSiguiente() {
    const page = this.leadsPage();
    if (page && !page.last) this.cargarLeads(this.paginaActual() + 1);
  }
}
