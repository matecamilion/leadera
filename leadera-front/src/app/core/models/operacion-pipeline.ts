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
  fechaCierre?: string | null;
}

export const ESTADOS_PIPELINE: EstadoOperacion[] = [
  'ABIERTA',
  'EN_GESTION',
  'RESERVADA',
];

export const TRANSICIONES_PERMITIDAS: Record<EstadoOperacion, EstadoOperacion[]> = {
  ABIERTA: ['EN_GESTION', 'CANCELADA'],
  EN_GESTION: ['ABIERTA', 'RESERVADA', 'CANCELADA'],
  RESERVADA: ['EN_GESTION', 'CERRADA_GANADA', 'CANCELADA'],
  CERRADA_GANADA: [],
  CANCELADA: [],
};
