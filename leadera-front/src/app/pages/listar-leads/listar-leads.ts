import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LeadService } from '../../core/services/lead-service'; // Ajusta la ruta a tu servicio
import { Lead } from '../../core/models/lead';
import { TiempoTranscurridoPipe } from '../../pipes/tiempo-transcurrido-pipe';
import { RouterModule } from '@angular/router';
import { LeadResumen } from '../../core/models/lead-resumen';
import { AuthService } from '../../core/services/auth-service';
import { ExcelService } from '../../core/services/excel-service';
import { NotificationService } from '../../core/services/notification-service';

@Component({
  selector: 'app-listado-leads',
  standalone: true,
  imports: [CommonModule, FormsModule, TiempoTranscurridoPipe, RouterModule],
  templateUrl: './listar-leads.html',
  styleUrls: ['./listar-leads.css'],
})
export class ListadoLeadsComponent implements OnInit {
  private servicioLead = inject(LeadService);
  private authService = inject(AuthService);
  private excelService = inject(ExcelService);
  private notificationService = inject(NotificationService);

  exportandoExcel = signal<boolean>(false);

  // Lead pendiente de confirmación en el modal de eliminar
  leadAEliminar = signal<LeadResumen | null>(null);

  // Signals
  leads = signal<LeadResumen[]>([]);
  cargando = signal<boolean>(true);
  filtroBusqueda = signal<string>('');
  estadoSeleccionado = signal<string>('TODOS');

  // Lógica reactiva para filtrar
  leadsFiltrados = computed(() => {
    let filtrados = this.leads();

    if (this.estadoSeleccionado() !== 'TODOS') {
      filtrados = filtrados.filter((l) => l.estado === this.estadoSeleccionado());
    }

    const busqueda = this.filtroBusqueda().toLowerCase().trim();
    if (busqueda) {
      filtrados = filtrados.filter(
        (l) =>
          l.nombre.toLowerCase().includes(busqueda) ||
          l.apellido.toLowerCase().includes(busqueda) ||
          l.telefono?.toLowerCase().includes(busqueda) ||
          l.email?.toLowerCase().includes(busqueda),
      );
    }

    return filtrados;
  });

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.cargando.set(true);
    this.servicioLead.getLeads(0, 500).subscribe({
      next: (resultado) => {
        this.leads.set(resultado.content);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  // Helper para obtener el detalle de la última interacción sin romper el template
  obtenerUltimoDetalle(lead: LeadResumen): string {
    return lead.ultimaInteraccion || 'Sin interacciones registradas';
  }

  contarOperacionesVenta(lead: LeadResumen): number {
    return lead.operacionesVenta;
  }

  contarOperacionesCompra(lead: LeadResumen): number {
    return lead.operacionesCompra;
  }

  esVencido(fecha: string | null): boolean {
    if (!fecha) return false;
    return new Date(fecha) < new Date();
  }

  formatearSeguimiento(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  abrirModalEliminar(lead: LeadResumen, modal: HTMLDialogElement) {
    this.leadAEliminar.set(lead);
    modal.showModal();
  }

  eliminarLead(modal: HTMLDialogElement) {
    const lead = this.leadAEliminar();
    if (!lead) return;

    this.servicioLead.eliminarLead(lead.id).subscribe({
      next: () => {
        // Sacamos el lead de la lista local: no hace falta recargar todo el listado.
        this.leads.update((list) => list.filter((l) => l.id !== lead.id));
        this.leadAEliminar.set(null);
        modal.close();
        this.notificationService.exito('Lead eliminado correctamente.');
      },
      error: () => {
        this.leadAEliminar.set(null);
        modal.close();
      },
    });
  }

  exportarExcel(): void {
    if (this.exportandoExcel()) return;
    const filtrados = this.leadsFiltrados();
    if (filtrados.length === 0) return;

    this.exportandoExcel.set(true);
    // Diferido un tick para que el DOM repinte el estado "Generando..." antes
    // de la generación síncrona del archivo (puede congelar el UI con muchos leads).
    setTimeout(() => {
      try {
        const nombre = this.authService.getNombreAgente() || 'Agente';
        this.excelService.exportarLeads(filtrados, nombre);
      } finally {
        this.exportandoExcel.set(false);
      }
    }, 0);
  }
}
