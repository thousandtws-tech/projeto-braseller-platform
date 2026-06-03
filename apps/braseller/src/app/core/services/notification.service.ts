import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  NotificationMessage,
  NotificationPreference,
  PreferenceRequest,
  ClearReadResponse,
  TenantNewSaleSummary,
} from '../models/notification.model';

/**
 * NotificationService — conecta ao notification-service via gateway
 *
 * Roteamento gateway → notification-service (porta 8083):
 *   GET   {apiUrl}/notifications/tenants/{id}                       → lista notificações
 *   PATCH {apiUrl}/notifications/tenants/{id}/{notifId}/read        → marcar como lida
 *   POST  {apiUrl}/notifications/tenants/{id}/clear-read            → arquivar todas lidas
 *   GET   {apiUrl}/notifications/tenants/{id}/preferences           → preferências de alerta
 *   PUT   {apiUrl}/notifications/tenants/{id}/preferences           → salvar preferências
 *   GET   {apiUrl}/notifications/tenants/{id}/new-sale-summary      → resumo de novas vendas
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly api = inject(ApiService);

  getNotifications(tenantId: string, limit?: number): Observable<NotificationMessage[]> {
    const params = limit ? { limit } : {};
    return this.api.get<NotificationMessage[]>(
      `notifications/tenants/${tenantId}`,
      params as Record<string, number>
    );
  }

  markAsRead(tenantId: string, notificationId: string): Observable<NotificationMessage> {
    return this.api.patch<NotificationMessage>(
      `notifications/tenants/${tenantId}/${notificationId}/read`,
      {}
    );
  }

  clearReadNotifications(tenantId: string): Observable<ClearReadResponse> {
    return this.api.post<ClearReadResponse>(
      `notifications/tenants/${tenantId}/clear-read`,
      {}
    );
  }

  getPreferences(tenantId: string): Observable<NotificationPreference> {
    return this.api.get<NotificationPreference>(
      `notifications/tenants/${tenantId}/preferences`
    );
  }

  updatePreferences(tenantId: string, request: PreferenceRequest): Observable<NotificationPreference> {
    return this.api.put<NotificationPreference>(
      `notifications/tenants/${tenantId}/preferences`,
      request
    );
  }

  getNewSaleSummary(tenantId: string): Observable<TenantNewSaleSummary> {
    return this.api.get<TenantNewSaleSummary>(
      `notifications/tenants/${tenantId}/new-sale-summary`
    );
  }
}
