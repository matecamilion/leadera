import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PropiedadService, PropiedadResumen } from '../../core/services/propiedad-service';

@Component({
  selector: 'app-propiedades',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './propiedades.html',
  styleUrl: './propiedades.css',
})
export class PropiedadesComponent implements OnInit {
  private propiedadService = inject(PropiedadService);

  propiedades = signal<PropiedadResumen[]>([]);
  cargando = signal<boolean>(true);
  error = signal<string>('');

  textoBusqueda = signal<string>('');
  filtroEstado = signal<string>('');

  readonly estadosPropiedad: PropiedadResumen['estado'][] = ['DISPONIBLE', 'RESERVADA', 'VENDIDA'];

  propiedadesFiltradas = computed(() => {
    const texto = this.textoBusqueda().toLowerCase().trim();
    const estado = this.filtroEstado();
    return this.propiedades().filter((p) => {
      const coincideTexto =
        !texto ||
        p.titulo?.toLowerCase().includes(texto) ||
        p.leadNombre?.toLowerCase().includes(texto);
      const coincideEstado = !estado || p.estado === estado;
      return coincideTexto && coincideEstado;
    });
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.propiedadService.obtenerTodas().subscribe({
      next: (data) => {
        this.propiedades.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar propiedades', err);
        this.error.set('No se pudieron cargar las propiedades.');
        this.cargando.set(false);
      },
    });
  }

  onBusquedaChange(valor: string): void {
    this.textoBusqueda.set(valor);
  }

  onEstadoChange(valor: string): void {
    this.filtroEstado.set(valor);
  }

  trackById(_: number, p: PropiedadResumen): number {
    return p.id;
  }
}
