import { ApplicationConfig,LOCALE_ID, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import localeEs from '@angular/common/locales/es';
import { authInterceptor } from './interceptors/auth-interceptor';

// Registramos el español
registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    { provide: LOCALE_ID, useValue: 'es-AR' }, // 'es-AR' para Argentina
    provideHttpClient(
      withInterceptors([authInterceptor]) 
    )
  ]
};
