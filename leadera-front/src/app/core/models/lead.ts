import { Interaccion } from './interaccion';
import { Propiedad } from './propiedad';
import { Operacion } from '../services/operacion-service';

export type EstadoLead = 'CALIENTE' | 'TIBIO' | 'FRIO' | 'INACTIVO';

export interface Lead {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  estado: EstadoLead;
  fechaEntrada?: string;
  ultimoContacto?: string | null;
  fechaProximoSeguimiento?: string | null;
  origen?: string;
  descripcionInicial?: string;
  interacciones?: Interaccion[];
  propiedades?: Propiedad[];
  operaciones?: Operacion[];
}
