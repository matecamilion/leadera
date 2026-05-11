import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { EventoPropiedad } from '../../core/models/evento-propiedad';

@Component({
  selector: 'app-propiedad-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './propiedad-detalle.html',
  styleUrl: './propiedad-detalle.css'
})
export class PropiedadDetalle implements OnInit {
  private route = inject(ActivatedRoute);
  private propiedadService = inject(PropiedadService);

  propiedad = signal<Propiedad | null>(null);
  eventos = signal<EventoPropiedad[]>([]);
  nuevoEvento: Partial<EventoPropiedad> = {};
  mostrarFormEvento = false;

  @Input('propiedad') set propiedadInput(value: Propiedad | null) {
    if (value) {
      this.propiedad.set(value);

      if (value.id) {
        this.cargarEventos(value.id);
      }
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

    this.cargarEventos(id);
  }

  cargarEventos(id: number) {
    this.propiedadService.obtenerEventos(id).subscribe({
      next: (e) => this.eventos.set(e)
    });
  }

  registrarEvento() {
    const propiedadActual = this.propiedad();

    if (!propiedadActual) {
      return;
    }

    this.propiedadService.registrarEvento(propiedadActual.id, this.nuevoEvento).subscribe({
      next: (e) => {
        this.eventos.update(list => [...list, e]);
        this.nuevoEvento = {};
        this.mostrarFormEvento = false;
      }
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
}