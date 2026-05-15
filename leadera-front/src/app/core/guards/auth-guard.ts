import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth-service';
import { inject } from '@angular/core';


export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();

  if (!token) {
    router.navigate(['/login']);
    return false;
  }

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp || payload.exp * 1000 <= Date.now()) {
      authService.logout();
      router.navigate(['/login']);
      return false;
    }
    return true;
  } catch {
    authService.logout();
    router.navigate(['/login']);
    return false;
  }
};
