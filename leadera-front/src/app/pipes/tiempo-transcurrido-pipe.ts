import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tiempoTranscurrido',
  standalone: true
})
export class TiempoTranscurridoPipe implements PipeTransform {
  transform(fecha: string | null | undefined): string {
    if (!fecha) return 'Sin contacto';

    const ahora = new Date();
    const fechaPasada = new Date(fecha);
    const diferenciaEnMs = ahora.getTime() - fechaPasada.getTime();
    
    const segundos = Math.floor(diferenciaEnMs / 1000);
    const minutos = Math.floor(segundos / 60);
    const horas = Math.floor(minutos / 60);
    const dias = Math.floor(horas / 24);

    if (dias === 0) {
      if (horas === 0) return 'Hace instantes';
      return ` ${horas} ${horas === 1 ? 'hora' : 'horas'}`;
    }
    if (dias === 1) return 'Ayer';
    if (dias < 30) return ` ${dias} días`;
    
    return fechaPasada.toLocaleDateString(); // Si pasó más de un mes, mostramos fecha real
  }
}
