import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CrearInteraccionRequest {
  detalle: string;
  tipoInteraccion: string;
}

@Injectable({
  providedIn: 'root',
})
export class InteraccionService {
  private apiUrl = `${environment.apiUrl}/leads`;

  constructor(private http: HttpClient) {}

  crearInteraccion(
    leadId: number,
    body: CrearInteraccionRequest,
    proximoContacto?: string,
  ): Observable<any> {
    let params = new HttpParams();

    // Si viene la fecha, la agregamos a los parámetros de la URL
    if (proximoContacto) {
      const fecha = proximoContacto.length === 16 ? proximoContacto + ':00' : proximoContacto;
      params = params.append('proximoContacto', fecha);
    }

    return this.http.post(`${this.apiUrl}/${leadId}/interacciones`, body, { params });
  }
}
