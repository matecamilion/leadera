import { Interaccion } from './interaccion';

export interface Lead {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  estado: string;

  fechaEntrada?: string;
  ultimoContacto?: string | null;

  interacciones?: Interaccion[];

  operaciones?: any[];
}