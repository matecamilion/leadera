import { Component, inject, OnInit, signal } from '@angular/core';
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

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.propiedadService.obtenerPorId(id).subscribe({
      next: (p) => this.propiedad.set(p)
    });
    this.propiedadService.obtenerEventos(id).subscribe({
      next: (e) => this.eventos.set(e)
    });
  }

  registrarEvento() {
    this.propiedadService.registrarEvento(this.propiedad()!.id, this.nuevoEvento).subscribe({
      next: (e) => {
        this.eventos.update(list => [...list, e]);
        this.nuevoEvento = {};
        this.mostrarFormEvento = false;
      }
    });
  }

  cambiarEstado(estado: string) {
    this.propiedadService.actualizarEstado(this.propiedad()!.id, estado).subscribe({
      next: () => this.propiedad.update(p => ({ ...p!, estado: estado as any }))
    });
  }
}