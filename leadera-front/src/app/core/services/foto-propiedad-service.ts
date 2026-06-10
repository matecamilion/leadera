import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FotoPropiedad } from '../models/foto-propiedad';

@Injectable({
  providedIn: 'root',
})
export class FotoPropiedadService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/propiedades`;

  /**
   * Sube el archivo al backend, que lo guarda en Supabase Storage con la
   * service role key y persiste la URL. El frontend ya no habla con Storage
   * ni necesita la anon key en el bundle.
   */
  async subirFoto(file: File, propiedadId: number, orden: number): Promise<FotoPropiedad> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('orden', orden.toString());

    return firstValueFrom(
      this.http.post<FotoPropiedad>(
        `${this.apiUrl}/${propiedadId}/fotos/upload`,
        formData,
        // Sin Content-Type manual: el browser lo setea con el boundary correcto.
      ),
    );
  }

  obtenerFotos(propiedadId: number): Observable<FotoPropiedad[]> {
    return this.http.get<FotoPropiedad[]>(`${this.apiUrl}/${propiedadId}/fotos`);
  }

  eliminarFoto(propiedadId: number, fotoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${propiedadId}/fotos/${fotoId}`);
  }
}
