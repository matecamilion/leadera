export interface LeadHoy {
  id: number;
  nombre: string;
  apellido: string;
  email?: string | null;
  estado: string;
  ultimoContacto?: string | null;
  ultimaInteraccion?: string | null;
}
