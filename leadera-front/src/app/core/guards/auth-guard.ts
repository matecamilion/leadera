import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth-service';
import { inject } from '@angular/core';


export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si hay un token en el localStorage, lo dejamos pasar
  if (authService.getToken()) {
    return true;
  }

  // Si no hay token, lo mandamos al login y bloqueamos la entrada
  router.navigate(['/login']);
  return false;
};
