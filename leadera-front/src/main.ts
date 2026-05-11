import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { provideHttpClient } from '@angular/common/http';
import { App } from './app/app';

bootstrapApplication(App, {
  ...appConfig,
  providers: [
    ...(appConfig.providers ?? []),
    provideHttpClient()
  ]
}).catch((err) => console.error(err));
