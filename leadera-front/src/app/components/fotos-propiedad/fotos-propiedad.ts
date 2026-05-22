import { Component, HostListener, computed, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { FotoPropiedadService } from '../../core/services/foto-propiedad-service';
import { FotoPropiedad } from '../../core/models/foto-propiedad';

const MAX_BYTES = 5 * 1024 * 1024; // 5 MB
const MAX_FOTOS = 10;

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
  @Input() readonly = false;

  fotos = signal<FotoPropiedad[]>([]);
  subiendo = signal(false);
  progreso = signal<{ actual: number; total: number } | null>(null);
  error = signal<string | null>(null);

  // Lightbox: null = cerrado, número = índice de la foto visible
  lightboxIndex = signal<number | null>(null);
  fotoActual = computed(() => {
    const i = this.lightboxIndex();
    if (i === null) return null;
    return this.fotos()[i] ?? null;
  });

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
    const files = input.files ? Array.from(input.files) : [];
    if (files.length === 0) return;

    this.error.set(null);

    // Validación de cupo total antes de tocar nada.
    const restantes = MAX_FOTOS - this.fotos().length;
    if (files.length > restantes) {
      this.error.set(`Límite de ${MAX_FOTOS} fotos. Podés agregar ${restantes} más.`);
      input.value = '';
      return;
    }

    // Validación por archivo. Si uno falla, abortamos el batch entero.
    for (const file of files) {
      if (!file.type.startsWith('image/')) {
        this.error.set(`El archivo "${file.name}" no es una imagen.`);
        input.value = '';
        return;
      }
      if (file.size > MAX_BYTES) {
        this.error.set(`"${file.name}" supera el máximo de 5 MB.`);
        input.value = '';
        return;
      }
    }

    this.subiendo.set(true);
    this.progreso.set({ actual: 0, total: files.length });

    let completados = 0;
    const ordenBase = this.fotos().length;

    const promesas = files.map(async (file, idx) => {
      try {
        const url = await this.fotoService.subirFotoASupabase(file, this.propiedadId);
        const foto = await firstValueFrom(
          this.fotoService.guardarFotoEnBackend(this.propiedadId, url, ordenBase + idx),
        );
        return foto;
      } finally {
        completados++;
        this.progreso.set({ actual: completados, total: files.length });
      }
    });

    const resultados = await Promise.allSettled(promesas);
    const fallidos = resultados.filter((r) => r.status === 'rejected').length;

    this.subiendo.set(false);
    this.progreso.set(null);
    input.value = '';

    // Refresh desde backend: garantiza orden estable después de uploads en paralelo.
    this.cargar();

    if (fallidos > 0) {
      this.error.set(`${fallidos} de ${files.length} fotos no se pudieron subir.`);
    }
  }

  eliminar(foto: FotoPropiedad, event?: Event): void {
    event?.stopPropagation();
    if (!confirm('¿Eliminar esta foto?')) return;
    this.fotoService.eliminarFoto(this.propiedadId, foto.id).subscribe({
      next: () => this.fotos.update((list) => list.filter((f) => f.id !== foto.id)),
      error: () => this.error.set('No se pudo eliminar la foto.'),
    });
  }

  // ---------- Lightbox ----------

  abrirLightbox(i: number): void {
    this.lightboxIndex.set(i);
  }

  cerrarLightbox(): void {
    this.lightboxIndex.set(null);
  }

  anteriorFoto(event?: Event): void {
    event?.stopPropagation();
    const i = this.lightboxIndex();
    const total = this.fotos().length;
    if (i === null || total === 0) return;
    this.lightboxIndex.set((i - 1 + total) % total);
  }

  siguienteFoto(event?: Event): void {
    event?.stopPropagation();
    const i = this.lightboxIndex();
    const total = this.fotos().length;
    if (i === null || total === 0) return;
    this.lightboxIndex.set((i + 1) % total);
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    if (this.lightboxIndex() === null) return;
    if (e.key === 'Escape') {
      this.cerrarLightbox();
    } else if (e.key === 'ArrowLeft') {
      this.anteriorFoto();
    } else if (e.key === 'ArrowRight') {
      this.siguienteFoto();
    }
  }
}
