export interface LeadResumen {
  id: number;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  estado: string;
  origen: string | null;
  fechaEntrada: string;
  ultimoContacto: string;
  fechaProximoSeguimiento: string;
  operacionesVenta: number;
  operacionesCompra: number;
  operacionesAlquiler: number;
  cantidadInteracciones: number;
  ultimaInteraccion: string | null;
}