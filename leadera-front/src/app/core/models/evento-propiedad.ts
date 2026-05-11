export interface EventoPropiedad {
  id: number;
  tipo: 'VISITA' | 'CONSULTA';
  fecha: string;
  detalle: string;
}