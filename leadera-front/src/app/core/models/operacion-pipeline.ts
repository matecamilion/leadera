import { TipoOperacion, EstadoOperacion } from '../services/operacion-service';

export interface OperacionPipeline {
  id: number;
  titulo?: string;
  estado: EstadoOperacion;
  tipoOperacion: TipoOperacion;
  leadId: number;
  nombreLead: string;
  apellidoLead: string;
  descripcionPropiedad: string;
  precioEstimado: string;
  fechaCreacion?: string | null;
  fechaCierre?: string | null;
}

export const ESTADOS_PIPELINE: EstadoOperacion[] = [
  'PUBLICADA',
  'RESERVADA',
  'EN_NEGOCIACION',
  'CERRADA_GANADA',
  'CANCELADA',
];

export const TRANSICIONES_PERMITIDAS: Record<EstadoOperacion, EstadoOperacion[]> = {
  PUBLICADA: ['RESERVADA', 'CANCELADA'],
  RESERVADA: ['PUBLICADA', 'EN_NEGOCIACION', 'CANCELADA'],
  EN_NEGOCIACION: ['RESERVADA', 'CERRADA_GANADA', 'CANCELADA'],
  CERRADA_GANADA: [],
  CANCELADA: [],
};
