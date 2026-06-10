import { HttpInterceptorFn } from '@angular/common/http';

// Solo login y register son públicos: ahí un token viejo o firmado con un
// secret distinto rompe la request. El resto de /auth (cambiar-password)
// REQUIERE el token como cualquier endpoint protegido.
const RUTAS_AUTH_PUBLICAS = ['/auth/login', '/auth/register'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (RUTAS_AUTH_PUBLICAS.some(ruta => req.url.includes(ruta))) {
    return next(req);
  }

  const token = localStorage.getItem('token');

  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  return next(req);
};
