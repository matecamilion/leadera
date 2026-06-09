import { Component, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification-service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  templateUrl: './toast-container.html',
  styleUrl: './toast-container.css',
})
export class ToastContainer {
  private notificationService = inject(NotificationService);

  toasts = this.notificationService.toasts;

  remover(id: number) {
    this.notificationService.remover(id);
  }
}
