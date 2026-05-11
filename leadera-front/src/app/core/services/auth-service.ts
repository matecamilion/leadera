import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';

  currentUserSig = signal<string | null>(localStorage.getItem('agente_nombre'));

  constructor(private http: HttpClient){};

  registrar(agente: any): Observable<any>{
    return this.http.post(`${this.apiUrl}/register`, agente);
  }

  

 login(credentials: any): Observable<any> {
  return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
    tap(res => {
      const nombreCompleto = `${res.nombre} ${res.apellido}`;
        localStorage.setItem('token', res.token);
        localStorage.setItem('agente_nombre', nombreCompleto);
        localStorage.setItem('agente_email', res.email);

      this.currentUserSig.set(nombreCompleto);
    })
  );
}


 getNombreAgente() { return this.currentUserSig(); }
  getEmailAgente() { return localStorage.getItem('agente_email'); }

  getIdAgente(): number | null {
  const token = localStorage.getItem('token');
  if (!token) return null;
  
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.id; // una vez que agregues el claim en el backend
}

  // Método de logout para limpiar todo
  logout(): void {
    localStorage.clear(); // Borra todo para evitar conflictos
  }

  getToken() {
    return localStorage.getItem('token');
  }

}
