import { Component, ElementRef, OnDestroy, effect, input, signal, viewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { LeadHoy } from '../../core/models/lead-hoy';
import { LeadCard } from '../lead-card/lead-card';

@Component({
  selector: 'app-lead-section',
  standalone: true,
  imports: [LeadCard, TitleCasePipe, RouterModule],
  templateUrl: './lead-section.html',
  styleUrl: './lead-section.css',
})
export class LeadSection implements OnDestroy {
  title = input.required<string>();
  subtitle = input.required<string>();
  badgeText = input.required<string>();
  variant = input.required<'prioritarios' | 'seguimientos' | 'nuevos' | 'completados'>();
  leads = input.required<LeadHoy[]>();
  totalReal = input<number | null>(null);
  verTodosRuta = input<string | null>(null);

  viewport = viewChild<ElementRef<HTMLDivElement>>('viewport');
  canScrollLeft = signal(false);
  canScrollRight = signal(false);

  private resizeObserver?: ResizeObserver;

  constructor(private router: Router) {
    effect(() => {
      this.leads();
      const el = this.viewport()?.nativeElement;
      if (!el) return;

      if (!this.resizeObserver) {
        this.resizeObserver = new ResizeObserver(() => this.evaluarOverflow());
        this.resizeObserver.observe(el);
      }
      queueMicrotask(() => this.evaluarOverflow());
    });
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  onScroll(): void {
    this.evaluarOverflow();
  }

  scrollPrev(): void {
    const el = this.viewport()?.nativeElement;
    if (!el) return;
    el.scrollBy({ left: -el.clientWidth * 0.85, behavior: 'smooth' });
  }

  scrollNext(): void {
    const el = this.viewport()?.nativeElement;
    if (!el) return;
    el.scrollBy({ left: el.clientWidth * 0.85, behavior: 'smooth' });
  }

  private evaluarOverflow(): void {
    const el = this.viewport()?.nativeElement;
    if (!el) return;
    this.canScrollLeft.set(el.scrollLeft > 0);
    this.canScrollRight.set(Math.ceil(el.scrollLeft + el.clientWidth) < el.scrollWidth);
  }

  iniciales(lead: LeadHoy): string {
    return `${lead.nombre.charAt(0)}${lead.apellido.charAt(0)}`.toUpperCase();
  }

  claseAvatar(estado: string): string {
    switch (estado) {
      case 'CALIENTE': return 'av-caliente';
      case 'TIBIO':    return 'av-tibio';
      case 'FRIO':     return 'av-frio';
      default:         return 'av-inactivo';
    }
  }

  clasePill(estado: string): string {
    switch (estado) {
      case 'CALIENTE': return 'pill-caliente';
      case 'TIBIO':    return 'pill-tibio';
      case 'FRIO':     return 'pill-frio';
      default:         return 'pill-inactivo';
    }
  }

  claseBordeTemperatura(estado: string): string {
    switch (estado) {
      case 'CALIENTE': return 'borde-caliente';
      case 'TIBIO':    return 'borde-tibio';
      case 'FRIO':     return 'borde-frio';
      default:         return 'borde-inactivo';
    }
  }

  tiempoDesde(ultimoContacto?: string | null): string {
    if (!ultimoContacto) return 'Sin contacto';
    const fecha = new Date(ultimoContacto);
    const ahora = new Date();
    const diffMs = Math.max(0, ahora.getTime() - fecha.getTime());
    const diffMin = Math.floor(diffMs / (1000 * 60));
    const diffHoras = Math.floor(diffMin / 60);
    const diffDias = Math.floor(diffHoras / 24);
    if (diffMin < 1) return 'Recién';
    if (diffMin < 60) return `Hace ${diffMin} min`;
    if (diffHoras < 24) return diffHoras === 1 ? 'Hace 1 hora' : `Hace ${diffHoras} horas`;
    if (diffDias === 1) return 'Hace 1 día';
    return `Hace ${diffDias} días`;
  }

  irADetalle(id: number): void {
    this.router.navigate(['/leads', id]);
  }

  irAInteraccion(id: number): void {
    this.router.navigate(['/leads', id, 'interaccion']);
  }
}
