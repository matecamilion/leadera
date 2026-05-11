export interface LeadResumen {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  estado: string;
  fechaEntrada: string;
  ultimoContacto: string;
  fechaProximoSeguimiento: string;
  operacionesVenta: number;
  operacionesCompra: number;
  cantidadInteracciones: number;
  ultimaInteraccion: string | null;
}