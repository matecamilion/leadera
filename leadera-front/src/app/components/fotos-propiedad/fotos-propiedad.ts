import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FotoPropiedadService } from '../../core/services/foto-propiedad-service';
import { FotoPropiedad } from '../../core/models/foto-propiedad';

const MAX_BYTES = 5 * 1024 * 1024; // 5 MB

@Component({
  selector: 'app-fotos-propiedad',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fotos-propiedad.html',
  styleUrl: './fotos-propiedad.css',
})
export class FotosPropiedadComponent implements OnInit {
  private fotoService = inject(FotoPropiedadService);

  @Input({ required: true }) propiedadId!: number;

  fotos = signal<FotoPropiedad[]>([]);
  subiendo = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.fotoService.obtenerFotos(this.propiedadId).subscribe({
      next: (fotos) => this.fotos.set(fotos),
      error: () => this.error.set('No se pudieron cargar las fotos.'),
    });
  }

  async onArchivoSeleccionado(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.error.set(null);

    if (!file.type.startsWith('image/')) {
      this.error.set('El archivo debe ser una imagen.');
      input.value = '';
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('La imagen supera el máximo de 5 MB.');
      input.value = '';
      return;
    }
    if (this.fotos().length >= 10) {
      this.error.set('Máximo 10 fotos por propiedad.');
      input.value = '';
      return;
    }

    this.subiendo.set(true);
    try {
      const url = await this.fotoService.subirFotoASupabase(file, this.propiedadId);
      const ordenSugerido = this.fotos().length;
      this.fotoService.guardarFotoEnBackend(this.propiedadId, url, ordenSugerido).subscribe({
        next: (foto) => {
          this.fotos.update((list) => [...list, foto]);
          this.subiendo.set(false);
        },
        error: (err) => {
          this.subiendo.set(false);
          this.error.set(err?.error?.message || 'No se pudo guardar la foto en el servidor.');
        },
      });
    } catch (err: any) {
      this.subiendo.set(false);
      this.error.set(err?.message || 'No se pudo subir la foto.');
    } finally {
      input.value = '';
    }
  }

  eliminar(foto: FotoPropiedad): void {
    if (!confirm('¿Eliminar esta foto?')) return;
    this.fotoService.eliminarFoto(this.propiedadId, foto.id).subscribe({
      next: () => this.fotos.update((list) => list.filter((f) => f.id !== foto.id)),
      error: () => this.error.set('No se pudo eliminar la foto.'),
    });
  }
}
