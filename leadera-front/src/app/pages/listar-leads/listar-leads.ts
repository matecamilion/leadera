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

  exportandoExcel = signal<boolean>(false);

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
    this.servicioLead.getLeads().subscribe({
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
