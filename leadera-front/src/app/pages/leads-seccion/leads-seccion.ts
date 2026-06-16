import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { LeadHoy } from '../../core/models/lead-hoy';
import { LeadCard } from '../../shared/lead-card/lead-card';

@Component({
  selector: 'app-leads-seccion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LeadCard],
  templateUrl: './leads-seccion.html',
  styleUrl: './leads-seccion.css',
})
export class LeadsSeccion implements OnInit {
  private route = inject(ActivatedRoute);
  private leadService = inject(LeadService);

  seccion = signal<string>('');
  leads = signal<LeadHoy[]>([]);
  totalElementos = signal(0);
  totalPaginas = signal(0);
  cargando = signal(true);
  error = signal('');

  busqueda = signal('');
  filtroOperacion = signal('');
  paginaActual = signal(0);
  readonly porPagina = 20;

  readonly titulos: Record<string, { titulo: string; subtitulo: string }> = {
    prioritarios: {
      titulo: 'Leads prioritarios',
      subtitulo: 'Calientes sin contacto por más de 3 días — atención inmediata'
    },
    nuevos: {
      titulo: 'Nuevos leads',
      subtitulo: 'Personas que consultaron y todavía no recibieron respuesta'
    },
    seguimientos: {
      titulo: 'Seguimientos de hoy',
      subtitulo: 'Contactos con recordatorio programado para hoy'
    },
  };

  infoSeccion = computed(() => {
    const sec = this.seccion();
    return this.titulos[sec] ?? { titulo: sec, subtitulo: '' };
  });

  paginas = computed(() =>
    Array.from({ length: this.totalPaginas() }, (_, i) => i + 1)
  );

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.seccion.set(params.get('seccion') ?? '');
      this.paginaActual.set(0);
      this.busqueda.set('');
      this.filtroOperacion.set('');
      this.cargar();
    });
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.leadService.getLeadsPorSeccion(
      this.seccion().toUpperCase(),
      this.busqueda(),
      this.filtroOperacion(),
      this.paginaActual(),
      this.porPagina
    ).subscribe({
      next: (res) => {
        this.leads.set(res.content);
        this.totalElementos.set(res.totalElements);
        this.totalPaginas.set(res.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los leads.');
        this.cargando.set(false);
      },
    });
  }

  onBuscar(): void {
    this.paginaActual.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.filtroOperacion.set('');
    this.paginaActual.set(0);
    this.cargar();
  }

  irPagina(n: number): void {
    if (n >= 0 && n < this.totalPaginas()) {
      this.paginaActual.set(n);
      this.cargar();
    }
  }

  hayFiltros = computed(() => !!this.busqueda() || !!this.filtroOperacion());
}
