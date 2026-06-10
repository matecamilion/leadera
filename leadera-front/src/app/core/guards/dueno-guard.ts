import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth-service';

// Restringe rutas al rol DUENO leyendo el claim del JWT. El backend igual
// valida con @PreAuthorize: esto es solo UX (no mostrar pantallas vacías).
export const duenoGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.esDueno()) {
    return true;
  }
  router.navigate(['/home']);
  return false;
};
