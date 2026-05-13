export interface Propiedad {
  id: number;
  direccion: string;
  precio: number;
  cantidadAmbientes: number;
  metrosTotales: number;
  metrosCubiertos: number;
  tipoVivienda: string;
  zona: string;
  observaciones: string;
  fechaPublicacion: string;
  diasEnMercado: number;
  estado: 'DISPONIBLE' | 'RESERVADA' | 'VENDIDA';
  lead?: { id: number };
}