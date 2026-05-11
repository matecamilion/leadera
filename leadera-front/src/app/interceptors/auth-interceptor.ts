import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
 // 1. Buscamos el token que se guarda en el login
  const token = localStorage.getItem('token');

  // 2. Si el token existe, clonamos la petición y le pegamos el Header
  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  // 3. Si no hay token (ej: en el login), la petición sigue viaje normal
  return next(req);
};
