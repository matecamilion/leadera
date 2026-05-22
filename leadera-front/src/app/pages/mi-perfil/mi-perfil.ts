import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AgenteService,
  AgentePerfilDTO,
  ActualizarPerfilRequest,
} from '../../core/services/agente-service';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mi-perfil.html',
  styleUrl: './mi-perfil.css',
})
export class MiPerfil implements OnInit {
  private fb = inject(FormBuilder);
  private agenteService = inject(AgenteService);

  perfil = signal<AgentePerfilDTO | null>(null);
  cargando = signal(true);
  editando = signal(false);
  guardando = signal(false);
  mensajeExito = signal<string | null>(null);
  error = signal<string | null>(null);

  form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, Validators.maxLength(80)]],
    metaMensualCierres: [12, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.agenteService.getPerfil().subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu perfil.');
        this.cargando.set(false);
      },
    });
  }

  iniciarEdicion(): void {
    const p = this.perfil();
    if (!p) return;
    this.form.reset({
      nombre: p.nombre,
      apellido: p.apellido,
      metaMensualCierres: p.metaMensualCierres,
    });
    this.error.set(null);
    this.mensajeExito.set(null);
    this.editando.set(true);
  }

  cancelar(): void {
    this.editando.set(false);
    this.error.set(null);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const data: ActualizarPerfilRequest = this.form.value;
    this.guardando.set(true);
    this.error.set(null);
    this.agenteService.actualizarPerfil(data).subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.editando.set(false);
        this.guardando.set(false);
        this.mensajeExito.set('Perfil actualizado correctamente.');
        setTimeout(() => this.mensajeExito.set(null), 3000);
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message || 'No se pudo guardar el perfil.');
      },
    });
  }

  esInvalido(campo: string): boolean {
    const c = this.form.get(campo);
    return !!(c && c.invalid && c.touched);
  }
}
