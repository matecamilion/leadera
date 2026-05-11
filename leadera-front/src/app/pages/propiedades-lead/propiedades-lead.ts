import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LeadService } from '../../core/services/lead-service';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';

@Component({
  selector: 'app-propiedades-lead',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './propiedades-lead.html',
  styleUrl: './propiedades-lead.css'
})
export class PropiedadesLead implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private leadService = inject(LeadService);
  private propiedadService = inject(PropiedadService);

  leadId = 0;
  lead = signal<any>(null);
  propiedades = signal<Propiedad[]>([]);
  cargando = signal(true);

  textoBusqueda = signal('');
  filtroTipo = signal('');
  filtroEstado = signal('');

  paginaActual = signal(1);
  readonly porPagina = 9;

  readonly tiposVivienda = ['CASA', 'DEPTO', 'PH', 'DUPLEX', 'TERRENO', 'OFICINA', 'LOCAL', 'GALPON'];
  readonly estadosPropiedad = ['DISPONIBLE', 'RESERVADA', 'VENDIDA'];

  propiedadesFiltradas = computed(() => {
    const texto = this.textoBusqueda().toLowerCase().trim();
    const tipo = this.filtroTipo();
    const estado = this.filtroEstado();

    return this.propiedades().filter(p => {
      const coincideTexto = !texto ||
        p.direccion?.toLowerCase().includes(texto) ||
        p.zona?.toLowerCase().includes(texto);
      const coincideTipo = !tipo || p.tipoVivienda === tipo;
      const coincideEstado = !estado || p.estado === estado;
      return coincideTexto && coincideTipo && coincideEstado;
    });
  });

  totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.propiedadesFiltradas().length / this.porPagina))
  );

  propiedadesPaginadas = computed(() => {
    const inicio = (this.paginaActual() - 1) * this.porPagina;
    return this.propiedadesFiltradas().slice(inicio, inicio + this.porPagina);
  });

  paginas = computed(() =>
    Array.from({ length: this.totalPaginas() }, (_, i) => i + 1)
  );

  hayFiltrosActivos = computed(() =>
    !!this.textoBusqueda() || !!this.filtroTipo() || !!this.filtroEstado()
  );

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.leadId = Number(id);
      this.leadService.getLeadById(this.leadId).subscribe({
        next: (data) => this.lead.set(data),
        error: (err) => console.error('Error al cargar el lead', err)
      });
      this.propiedadService.obtenerPorLead(this.leadId).subscribe({
        next: (data) => {
          this.propiedades.set(data);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false)
      });
    }
  }

  onBusquedaChange(texto: string) {
    this.textoBusqueda.set(texto);
    this.paginaActual.set(1);
  }

  onTipoChange(tipo: string) {
    this.filtroTipo.set(tipo);
    this.paginaActual.set(1);
  }

  onEstadoChange(estado: string) {
    this.filtroEstado.set(estado);
    this.paginaActual.set(1);
  }

  limpiarFiltros() {
    this.textoBusqueda.set('');
    this.filtroTipo.set('');
    this.filtroEstado.set('');
    this.paginaActual.set(1);
  }

  irAPagina(pagina: number) {
    if (pagina >= 1 && pagina <= this.totalPaginas()) {
      this.paginaActual.set(pagina);
    }
  }

  verPropiedad(id: number) {
    this.router.navigate(['/propiedades', id]);
  }
}
