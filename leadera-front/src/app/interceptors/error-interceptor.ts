import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let mensaje = 'Ocurrió un error inesperado.';

      if (error.error instanceof ErrorEvent) {
        mensaje = `Error de red: ${error.error.message}`;
      } else {
        switch (error.status) {
          case 0:
            mensaje = 'No se pudo conectar con el servidor.';
            break;
          case 400:
            mensaje = error.error?.message || 'Solicitud inválida.';
            break;
          case 401:
            mensaje = 'Sesión expirada. Iniciá sesión de nuevo.';
            localStorage.clear();
            router.navigate(['/login']);
            break;
          case 403:
            mensaje = 'No tenés permiso para realizar esta acción.';
            break;
          case 404:
            mensaje = error.error?.message || 'Recurso no encontrado.';
            break;
          case 409:
            mensaje = error.error?.message || 'Conflicto con el estado actual.';
            break;
          case 500:
            mensaje = 'Error interno del servidor.';
            break;
          default:
            mensaje = error.error?.message || `Error ${error.status}`;
        }
      }

      return throwError(() => ({ ...error, mensajeAmigable: mensaje }));
    }),
  );
};
