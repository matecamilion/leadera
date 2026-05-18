import { Component, computed, input } from '@angular/core';
import { LeadHoy } from '../../core/models/lead-hoy';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-lead-card',
  imports: [RouterLink],
  templateUrl: './lead-card.html',
  styleUrl: './lead-card.css',
})
export class LeadCard {
  lead = input.required<LeadHoy>();

  nombreCompleto = computed(() => {
    const l = this.lead();
    return `${l.nombre} ${l.apellido}`;
  });

  ultimaInteraccion = computed(() => {
    return this.lead().ultimaInteraccion ?? 'Sin interacciones registradas';
  });

  tiempoDesdeUltimoContacto = computed(() => {
    const ultimoContacto = this.lead().ultimoContacto;
    if (!ultimoContacto) return 'Sin contacto';

    const fecha = new Date(ultimoContacto);
    const ahora = new Date();

    // Si por desfase de zona horaria la fecha quedó en el "futuro", lo
    // tratamos como recién — nunca mostramos negativos.
    const diffMs = Math.max(0, ahora.getTime() - fecha.getTime());
    const diffMin = Math.floor(diffMs / (1000 * 60));
    const diffHoras = Math.floor(diffMin / 60);
    const diffDias = Math.floor(diffHoras / 24);

    if (diffMin < 1) return 'Recién';
    if (diffMin < 60) return `Hace ${diffMin} min`;
    if (diffHoras < 24) return diffHoras === 1 ? 'Hace 1 hora' : `Hace ${diffHoras} horas`;
    if (diffDias === 1) return 'Hace 1 día';
    return `Hace ${diffDias} días`;
  });

  claseEstado = computed(() => {
    switch (this.lead().estado) {
      case 'CALIENTE': return 'estado-caliente';
      case 'TIBIO': return 'estado-tibio';
      case 'FRIO': return 'estado-frio';
      case 'INACTIVO': return 'estado-inactivo';
      default: return '';
    }
  });

  textoEstado = computed(() => this.lead().estado);

  constructor(private router: Router) {}

  irANuevaInteraccion() {
    this.router.navigate(['/leads', this.lead().id, 'interaccion']);
  }
}
