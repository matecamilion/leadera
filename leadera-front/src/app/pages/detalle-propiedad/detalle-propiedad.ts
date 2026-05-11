import { Component, Input, Output, EventEmitter, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Propiedad } from '../../core/models/propiedad';
import { EventoPropiedad } from '../../core/models/evento-propiedad';
import { PropiedadService } from '../../core/services/propiedad-service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-detalle-propiedad',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './detalle-propiedad.html',
  styleUrl: './detalle-propiedad.css'
})
export class DetallePropiedadComponent implements OnInit {
  @Input() propiedad!: Propiedad;
  @Output() cerrar = new EventEmitter<void>();
  @Output() estadoActualizado = new EventEmitter<void>();

  private propiedadService = inject(PropiedadService);

  eventos = signal<EventoPropiedad[]>([]);
  nuevoEvento: Partial<EventoPropiedad> = {};
  mostrarFormEvento = false;

  ngOnInit() {
    this.propiedadService.obtenerEventos(this.propiedad.id).subscribe({
      next: (data) => this.eventos.set(data),
      error: (err) => console.error(err)
    });
  }

  registrarEvento() {
    this.propiedadService.registrarEvento(this.propiedad.id, this.nuevoEvento).subscribe({
      next: (e) => {
        this.eventos.update(list => [...list, e]);
        this.nuevoEvento = {};
        this.mostrarFormEvento = false;
      }
    });
  }

  cambiarEstado(estado: string) {
    this.propiedadService.actualizarEstado(this.propiedad.id, estado).subscribe({
      next: () => {
        this.propiedad = { ...this.propiedad, estado: estado as any };
        this.estadoActualizado.emit();
      }
    });
  }
}