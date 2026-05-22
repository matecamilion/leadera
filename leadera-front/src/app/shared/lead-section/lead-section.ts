import { Component, ElementRef, OnDestroy, effect, input, signal, viewChild } from '@angular/core';
import { LeadHoy } from '../../core/models/lead-hoy';
import { LeadCard } from '../lead-card/lead-card';

@Component({
  selector: 'app-lead-section',
  standalone: true,
  imports: [LeadCard],
  templateUrl: './lead-section.html',
  styleUrl: './lead-section.css',
})
export class LeadSection implements OnDestroy {
  title = input.required<string>();
  subtitle = input.required<string>();
  badgeText = input.required<string>();
  variant = input.required<'prioritarios' | 'seguimientos' | 'nuevos' | 'completados'>();
  leads = input.required<LeadHoy[]>();

  viewport = viewChild<ElementRef<HTMLDivElement>>('viewport');
  canScrollLeft = signal(false);
  canScrollRight = signal(false);

  private resizeObserver?: ResizeObserver;

  constructor() {
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
}
