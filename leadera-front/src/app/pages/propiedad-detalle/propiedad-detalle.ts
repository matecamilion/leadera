import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { FotosPropiedadComponent } from '../../components/fotos-propiedad/fotos-propiedad';
import { MatchingService, CompradorPotencial } from '../../core/services/matching-service';

@Component({
  selector: 'app-propiedad-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FotosPropiedadComponent],
  templateUrl: './propiedad-detalle.html',
  styleUrl: './propiedad-detalle.css'
})
export class PropiedadDetalle implements OnInit {
  private route = inject(ActivatedRoute);
  private propiedadService = inject(PropiedadService);
  private matchingService = inject(MatchingService);

  propiedad = signal<Propiedad | null>(null);

  // Edición
  edicion: Partial<Propiedad> = {};
  guardandoEdicion = signal<boolean>(false);
  errorEdicion = signal<string>('');

  // Matching de compradores potenciales
  compradores = signal<CompradorPotencial[]>([]);
  cargandoMatch = signal<boolean>(false);
  panelAbierto = signal<boolean>(false);
  errorMatch = signal<string>('');

  readonly tiposVivienda = ['CASA', 'DEPTO', 'PH', 'DUPLEX', 'TERRENO', 'OFICINA', 'LOCAL', 'GALPON'];

  @Input('propiedad') set propiedadInput(value: Propiedad | null) {
    if (value) {
      this.propiedad.set(value);
    }
  }

  @Output() cerrar = new EventEmitter<void>();
  @Output() estadoActualizado = new EventEmitter<void>();

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam) {
      return;
    }

    const id = Number(idParam);

    if (!id || Number.isNaN(id)) {
      return;
    }

    this.propiedadService.obtenerPorId(id).subscribe({
      next: (p) => this.propiedad.set(p)
    });
  }

  cambiarEstado(estado: string) {
    const propiedadActual = this.propiedad();

    if (!propiedadActual) {
      return;
    }

    this.propiedadService.actualizarEstado(propiedadActual.id, estado).subscribe({
      next: () => {
        this.propiedad.update(p => ({ ...p!, estado: estado as any }));
        this.estadoActualizado.emit();
      }
    });
  }

  abrirModalEdicion(modal: HTMLDialogElement) {
    const p = this.propiedad();
    if (!p) return;
    this.edicion = {
      direccion: p.direccion,
      precio: p.precio,
      cantidadAmbientes: p.cantidadAmbientes,
      metrosTotales: p.metrosTotales,
      metrosCubiertos: p.metrosCubiertos,
      tipoVivienda: p.tipoVivienda,
      zona: p.zona,
      observaciones: p.observaciones,
    };
    this.errorEdicion.set('');
    modal.showModal();
  }

  togglePanelCompradores() {
    if (this.panelAbierto()) {
      // Al cerrar, limpiamos los resultados para no mostrar datos viejos al re-abrir.
      this.panelAbierto.set(false);
      this.compradores.set([]);
      this.errorMatch.set('');
      return;
    }

    const p = this.propiedad();
    if (!p) return;

    this.panelAbierto.set(true);
    this.cargandoMatch.set(true);
    this.errorMatch.set('');

    this.matchingService.obtenerCompradores(p.id).subscribe({
      next: (lista) => {
        this.compradores.set(lista);
        this.cargandoMatch.set(false);
      },
      error: (err) => {
        this.cargandoMatch.set(false);
        this.errorMatch.set(
          err?.error?.message ?? 'No se pudieron cargar los compradores potenciales.',
        );
      },
    });
  }

  guardarEdicion(modal: HTMLDialogElement) {
    const p = this.propiedad();
    if (!p) return;

    const body: any = { ...this.edicion };

    this.guardandoEdicion.set(true);
    this.errorEdicion.set('');

    this.propiedadService.editar(p.id, body).subscribe({
      next: (actualizada) => {
        this.propiedad.set(actualizada);
        this.guardandoEdicion.set(false);
        modal.close();
      },
      error: (err) => {
        console.error('Error al editar propiedad', err);
        this.errorEdicion.set(err?.error?.message ?? 'No se pudo guardar los cambios.');
        this.guardandoEdicion.set(false);
      },
    });
  }
}