import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { FotosPropiedadComponent } from '../../components/fotos-propiedad/fotos-propiedad';

@Component({
  selector: 'app-propiedad-editar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FotosPropiedadComponent],
  templateUrl: './propiedad-editar.html',
  styleUrl: './propiedad-editar.css',
})
export class PropiedadEditar implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private propiedadService = inject(PropiedadService);

  propiedad = signal<Propiedad | null>(null);
  edicion: Partial<Propiedad> = {};
  guardando = signal<boolean>(false);
  error = signal<string>('');

  readonly tiposVivienda = ['CASA', 'DEPTO', 'PH', 'DUPLEX', 'TERRENO', 'OFICINA', 'LOCAL', 'GALPON'];

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    const id = Number(idParam);
    if (!id || Number.isNaN(id)) return;

    this.propiedadService.obtenerPorId(id).subscribe({
      next: (p) => {
        this.propiedad.set(p);
        this.edicion = {
          direccion: p.direccion,
          precio: p.precio,
          cantidadAmbientes: p.cantidadAmbientes,
          metrosTotales: p.metrosTotales,
          metrosCubiertos: p.metrosCubiertos,
          tipoVivienda: p.tipoVivienda,
          zona: p.zona,
          observaciones: p.observaciones,
          linkPortal: p.linkPortal,
        };
      },
      error: () => this.error.set('No se pudo cargar la propiedad.')
    });
  }

  guardar() {
    const p = this.propiedad();
    if (!p) return;

    this.guardando.set(true);
    this.error.set('');

    this.propiedadService.editar(p.id, this.edicion).subscribe({
      next: () => {
        this.guardando.set(false);
        this.router.navigate(['/propiedades', p.id]);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo guardar los cambios.');
        this.guardando.set(false);
      },
    });
  }

  cancelar() {
    const p = this.propiedad();
    if (p) this.router.navigate(['/propiedades', p.id]);
  }
}
