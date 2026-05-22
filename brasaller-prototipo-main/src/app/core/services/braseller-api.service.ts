import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {
  AppNotification,
  Connector,
  DashboardSummary,
  ManualOrderPayload,
  Order,
  Tenant,
  UserRole,
} from '../models/braseller.models';

@Injectable({providedIn: 'root'})
export class BrasellerApiService {
  private readonly http = inject(HttpClient);

  me() {
    return this.http.get<Tenant>('/api/auth/me');
  }

  login(payload: {email: string; password: string}) {
    return this.http.post<{success: boolean; user: Tenant}>('/api/auth/login', payload);
  }

  register(payload: {name: string; sellerName: string; email: string; password: string}) {
    return this.http.post<{success: boolean; user: Tenant}>('/api/auth/register', payload);
  }

  logout() {
    return this.http.post<{success: boolean}>('/api/auth/logout', {});
  }

  switchRole(role: UserRole) {
    return this.http.post<{success: boolean; user: Tenant}>('/api/auth/switch-role', {role});
  }

  connectors() {
    return this.http.get<Connector[]>('/api/connectors');
  }

  toggleConnector(key: string, active: boolean) {
    return this.http.post<{success: boolean}>('/api/connectors/toggle', {key, active});
  }

  syncConnectors() {
    return this.http.post<{success: boolean; addedCount: number; message?: string}>('/api/connectors/sync', {});
  }

  summary() {
    return this.http.get<DashboardSummary>('/api/summary');
  }

  orders(params: Record<string, string>) {
    return this.http.get<Order[]>('/api/orders', {params});
  }

  createOrder(payload: ManualOrderPayload) {
    return this.http.post<{success: boolean}>('/api/orders', payload);
  }

  notifications() {
    return this.http.get<AppNotification[]>('/api/notifications');
  }

  markNotificationAsRead(id: string) {
    return this.http.post<{success: boolean; notifications: AppNotification[]}>('/api/notifications/read', {id});
  }

  clearNotifications() {
    return this.http.post<{success: boolean}>('/api/notifications/clear', {});
  }

  upgrade(plan: string) {
    return this.http.post<{success: boolean; user: Tenant}>('/api/billing/upgrade', {plan});
  }

  askAssistant(message: string) {
    return this.http.post<{response: string; isReal: boolean}>('/api/gemini/chat', {message});
  }
}
