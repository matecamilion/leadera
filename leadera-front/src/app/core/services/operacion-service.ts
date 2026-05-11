import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Propiedad } from '../models/propiedad';
import { Busqueda } from '../models/busqueda';

export type TipoOperacion = 'VENTA' | 'COMPRA' | 'ALQUILER';
export type EstadoOperacion = 'ABIERTA' | 'EN_GESTION' | 'RESERVADA' | 'CERRADA_GANADA' | 'CANCELADA';

export interface Operacion {
  id: number;
  titulo: string;
  tipoOperacion: TipoOperacion;
  estadoOperacion: EstadoOperacion;
  descripcion: string;
  fechaCreacion: string;
  fechaCierre: string | null;
  fechaProximoSeguimiento: string | null;
  propiedad: Propiedad | null;
  busqueda: Busqueda | null;
}

export interface CrearOperacionRequest {
  titulo: string;
  tipoOperacion: TipoOperacion;
  descripcion: string;
  propiedad?: { id: number } | null;
  busqueda?: Busqueda | null;
}

@Injectable({
  providedIn: 'root',
})
export class OperacionService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  obtenerOperacionesDelLead(leadId: number): Observable<Operacion[]> {
    return this.http.get<Operacion[]>(`${this.apiUrl}/leads/${leadId}/operaciones`);
  }

  obtenerOperacionesAbiertasDelLead(leadId: number): Observable<Operacion[]> {
    return this.http.get<Operacion[]>(`${this.apiUrl}/leads/${leadId}/operaciones/abiertas`);
  }

  crearOperacion(leadId: number, body: CrearOperacionRequest): Observable<Operacion> {
    return this.http.post<Operacion>(`${this.apiUrl}/leads/${leadId}/operaciones`, body);
  }

  obtenerOperacionPorId(leadId: number, operacionId: number): Observable<Operacion> {
  return this.http.get<Operacion>(`${this.apiUrl}/leads/${leadId}/operaciones/${operacionId}`);
}

cambiarEstadoOperacion(
  leadId: number,
  operacionId: number,
  estadoOperacion: string
) {
  return this.http.patch<Operacion>(
    `${this.apiUrl}/leads/${leadId}/operaciones/${operacionId}/estado?estadoOperacion=${estadoOperacion}`,
    {}
  );
}
}