import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { Propiedad } from '../models/propiedad';
import { Busqueda } from '../models/busqueda';
import { OperacionPipeline } from '../models/operacion-pipeline';
import { EventoOperacion } from '../models/evento-operacion';
import { environment } from '../../../environments/environment';

export type TipoOperacion = 'VENTA' | 'COMPRA' | 'ALQUILER';
export type EstadoOperacion = 'ABIERTA' | 'PUBLICADA' | 'RESERVADA' | 'EN_NEGOCIACION' | 'CERRADA_GANADA' | 'CANCELADA';

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
  leadId?: number;
  montoOperacion?: number | null;
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
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  obtenerOperacionesDelLead(leadId: number): Observable<Operacion[]> {
    return this.http.get<Operacion[]>(`${this.apiUrl}/leads/${leadId}/operaciones`);
  }

  obtenerOperacionesDePropiedad(propiedadId: number): Observable<Operacion[]> {
    return this.http.get<Operacion[]>(`${this.apiUrl}/propiedades/${propiedadId}/operaciones`);
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

obtenerPipeline(): Observable<OperacionPipeline[]> {
  return this.http.get<OperacionPipeline[]>(`${this.apiUrl}/operaciones`);
}

obtenerOperacionesCerradas(): Observable<OperacionPipeline[]> {
  return this.http.get<OperacionPipeline[]>(`${this.apiUrl}/operaciones/cerradas`);
}

obtenerTodasLasOperaciones(): Observable<OperacionPipeline[]> {
  return forkJoin({
    abiertas: this.obtenerPipeline(),
    cerradas: this.obtenerOperacionesCerradas(),
  }).pipe(map(({ abiertas, cerradas }) => [...abiertas, ...cerradas]));
}

obtenerEventos(leadId: number, operacionId: number): Observable<EventoOperacion[]> {
  return this.http.get<EventoOperacion[]>(
    `${this.apiUrl}/leads/${leadId}/operaciones/${operacionId}/eventos`
  );
}

registrarEvento(
  leadId: number,
  operacionId: number,
  evento: Partial<EventoOperacion>
): Observable<EventoOperacion> {
  return this.http.post<EventoOperacion>(
    `${this.apiUrl}/leads/${leadId}/operaciones/${operacionId}/eventos`,
    evento
  );
}

cambiarEstadoPipeline(
  operacionId: number,
  estadoOperacion: EstadoOperacion
): Observable<OperacionPipeline> {
  return this.http.patch<OperacionPipeline>(
    `${this.apiUrl}/operaciones/${operacionId}/estado`,
    { estadoOperacion }
  );
}
}