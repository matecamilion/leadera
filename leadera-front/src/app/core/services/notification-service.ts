import { Injectable, signal } from '@angular/core';

export type ToastTipo = 'exito' | 'error' | 'info';

export interface Toast {
  id: number;
  mensaje: string;
  tipo: ToastTipo;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly toasts = signal<Toast[]>([]);
  private contador = 0;

  exito(mensaje: string) { this.agregar(mensaje, 'exito'); }
  error(mensaje: string) { this.agregar(mensaje, 'error'); }
  info(mensaje: string) { this.agregar(mensaje, 'info'); }

  private agregar(mensaje: string, tipo: ToastTipo) {
    const id = ++this.contador;
    this.toasts.update(t => [...t, { id, mensaje, tipo }]);
    setTimeout(() => this.remover(id), 4000);
  }

  remover(id: number) {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
