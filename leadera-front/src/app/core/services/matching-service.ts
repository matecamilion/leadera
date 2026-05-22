import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CompradorPotencial {
  leadId: number;
  leadNombre: string;
  leadTelefono: string;
  leadEstado: string;
  operacionId: number;
  busquedaZona: string;
  busquedaTipoVivienda: string;
  busquedaPrecioMin: number;
  busquedaPrecioMax: number;
  busquedaAmbientes: number;
  busquedaMetros: number;
}

@Injectable({
  providedIn: 'root',
})
export class MatchingService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/propiedades`;

  obtenerCompradores(propiedadId: number): Observable<CompradorPotencial[]> {
    return this.http.get<CompradorPotencial[]>(
      `${this.apiUrl}/${propiedadId}/compradores-potenciales`,
    );
  }
}
