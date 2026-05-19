import { EstadoLead } from './lead';

export interface CrearLeadRequest {
  nombre?: string | null;
  apellido?: string | null;
  telefono?: string | null;
  email?: string | null;
  estado?: EstadoLead | string | null;
  origen?: string | null;
  descripcionInicial?: string | null;
  fechaProximoSeguimiento?: string | null;
}
