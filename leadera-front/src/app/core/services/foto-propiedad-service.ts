import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FotoPropiedad } from '../models/foto-propiedad';

const BUCKET = 'fotos-propiedades';

@Injectable({
  providedIn: 'root',
})
export class FotoPropiedadService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/propiedades`;

  /**
   * Sube el archivo directo a Supabase Storage via fetch (sin @supabase/supabase-js).
   * Devuelve la URL pública del objeto subido.
   */
  async subirFotoASupabase(file: File, propiedadId: number): Promise<string> {
    const timestamp = Date.now();
    // Sanitizo el nombre para evitar problemas con caracteres especiales en la URL.
    const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const path = `${propiedadId}/${timestamp}-${safeName}`;

    const uploadUrl = `${environment.supabaseUrl}/storage/v1/object/${BUCKET}/${path}`;

    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${environment.supabaseAnonKey}`,
        apikey: environment.supabaseAnonKey,
        'Content-Type': file.type || 'application/octet-stream',
        'x-upsert': 'false',
      },
      body: file,
    });

    if (!response.ok) {
      const detalle = await response.text().catch(() => '');
      throw new Error(`Supabase Storage rechazó la subida (${response.status}): ${detalle}`);
    }

    return `${environment.supabaseUrl}/storage/v1/object/public/${BUCKET}/${path}`;
  }

  guardarFotoEnBackend(propiedadId: number, url: string, orden: number): Observable<FotoPropiedad> {
    return this.http.post<FotoPropiedad>(`${this.apiUrl}/${propiedadId}/fotos`, { url, orden });
  }

  obtenerFotos(propiedadId: number): Observable<FotoPropiedad[]> {
    return this.http.get<FotoPropiedad[]>(`${this.apiUrl}/${propiedadId}/fotos`);
  }

  eliminarFoto(propiedadId: number, fotoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${propiedadId}/fotos/${fotoId}`);
  }
}
