import { Component, computed, input, output, signal } from '@angular/core';
import { Lead } from '../../core/models/lead';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-lead-card',
  imports: [RouterLink],
  templateUrl: './lead-card.html',
  styleUrl: './lead-card.css',
})
export class LeadCard {
  lead = input.required<Lead>();
  tareaCompletada = output<number>();
  contactado = input<boolean>(false);

  completado = signal(false);

  nombreCompleto = computed(() => {
    const l = this.lead();
    return `${l.nombre} ${l.apellido}`;
  })

  ultimaInteraccion = computed(() => {
    const interacciones = this.lead().interacciones ?? [];
    if(interacciones.length === 0) return 'Sin interacciones registradas';

    const ultima = interacciones[interacciones.length - 1];
    return ultima.detalle ?? 'Sin detalle';
  });

  tiempoDesdeUltimoContacto = computed(() => {
    const ultimoContacto = this.lead().ultimoContacto;
    if(!ultimoContacto) return 'Sin contacto';

    const fecha = new Date(ultimoContacto);
    const ahora = new Date();

    const diffMes = ahora.getTime() - fecha.getTime();
    const diffHoras = Math.floor(diffMes / (1000 * 60 * 60));
    const diffDias = Math.floor(diffHoras / 24 );

    if(diffHoras < 24){
      return `Hace ${diffHoras} horas`;
    }

    if(diffDias === 1){
      return `Hace 1 dia`;
    }

    return `Hace ${diffDias} dias`;

  });

  claseEstado = computed(() => {
    switch (this.lead().estado) {
      case 'GANADO':
        return 'estado-ganado';
      case 'CALIENTE':
        return 'estado-caliente';
      case 'TIBIO':
        return 'estado-tibio';
      case 'FRIO':
        return 'estado-frio';
      case 'INACTIVO':
        return 'estado-inactivo';
      default:
        return '';
    }
  });

  textoEstado = computed(() => {
    switch (this.lead().estado) {
      case 'GANADO':
        return 'GANADO';
      case 'CALIENTE':
        return 'CALIENTE';
      case 'TIBIO':
        return 'TIBIO';
      case 'FRIO':
        return 'FRIO';
      case 'INACTIVO':
        return 'INACTIVO';
      default:
        return this.lead().estado;
    }
  });

   constructor(private router: Router) {}

irANuevaInteraccion() {
  this.router.navigate(['/leads', this.lead().id, 'interaccion']);
}



}
