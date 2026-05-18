export interface LeadHoy {
  id: number;
  nombre: string;
  apellido: string;
  estado: string;
  ultimoContacto?: string | null;
  ultimaInteraccion?: string | null;
}
