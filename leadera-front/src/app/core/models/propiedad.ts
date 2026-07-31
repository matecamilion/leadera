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
  /** URL de la publicación en el portal. Opcional. */
  linkPortal?: string | null;
  fechaPublicacion: string;
  diasEnMercado: number;
  estado: 'DISPONIBLE' | 'RESERVADA' | 'VENDIDA';
  leadId?: number;
}