import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  AuthenticateRequest,
  ConnectorDescriptor,
  ConnectorStatus,
  ConnectorToken,
  FeeInfo,
  InvoiceInfo,
  PaymentInfo,
  SyncAllRequest,
  SyncAccepted,
  SyncJob,
  StandardOrder,
  TenantContext,
} from '../models/connector.model';

/**
 * CoreService — conecta ao core-service via gateway
 *
 * Roteamento gateway → core-service (porta 8081):
 *   GET  {apiUrl}/core/context                               → contexto do tenant autenticado
 *   GET  {apiUrl}/core/connectors                            → descritores dos conectores disponíveis
 *   GET  {apiUrl}/core/connectors/{name}/status              → status de conexão do conector
 *   GET  {apiUrl}/core/connectors/{name}/orders              → pedidos padronizados do conector
 *   POST {apiUrl}/core/connectors/{name}/sync-all            → enfileira sincronização completa
 *   GET  {apiUrl}/core/connectors/sync-jobs/{jobId}          → status do job de sincronização
 */
@Injectable({ providedIn: 'root' })
export class CoreService {
  private readonly api = inject(ApiService);

  getTenantContext(): Observable<TenantContext> {
    return this.api.get<TenantContext>('core/context');
  }

  getConnectors(): Observable<ConnectorDescriptor[]> {
    return this.api.get<ConnectorDescriptor[]>('core/connectors');
  }

  getConnectorStatus(connectorName: string): Observable<ConnectorStatus> {
    return this.api.get<ConnectorStatus>(`core/connectors/${this.pathSegment(connectorName)}/status`);
  }

  authenticateConnector(connectorName: string, request: AuthenticateRequest): Observable<ConnectorToken> {
    return this.api.post<ConnectorToken>(`core/connectors/${this.pathSegment(connectorName)}/authenticate`, request);
  }

  refreshConnectorToken(connectorName: string): Observable<ConnectorToken> {
    return this.api.post<ConnectorToken>(`core/connectors/${this.pathSegment(connectorName)}/refresh-token`, {});
  }

  getConnectorOrders(
    connectorName: string,
    params: { from?: string; to?: string; status?: string; limit?: number } = {}
  ): Observable<StandardOrder[]> {
    return this.api.get<StandardOrder[]>(
      `core/connectors/${this.pathSegment(connectorName)}/orders`,
      params as Record<string, string | number | boolean>
    );
  }

  getConnectorOrderDetail(connectorName: string, orderId: string): Observable<StandardOrder> {
    return this.api.get<StandardOrder>(
      `core/connectors/${this.pathSegment(connectorName)}/orders/${this.pathSegment(orderId)}`
    );
  }

  getConnectorOrderPayments(connectorName: string, orderId: string): Observable<PaymentInfo[]> {
    return this.api.get<PaymentInfo[]>(
      `core/connectors/${this.pathSegment(connectorName)}/orders/${this.pathSegment(orderId)}/payments`
    );
  }

  getConnectorOrderFees(connectorName: string, orderId: string): Observable<FeeInfo[]> {
    return this.api.get<FeeInfo[]>(
      `core/connectors/${this.pathSegment(connectorName)}/orders/${this.pathSegment(orderId)}/fees`
    );
  }

  getConnectorInvoices(
    connectorName: string,
    params: { from?: string; to?: string; limit?: number } = {}
  ): Observable<InvoiceInfo[]> {
    return this.api.get<InvoiceInfo[]>(
      `core/connectors/${this.pathSegment(connectorName)}/invoices`,
      params as Record<string, string | number | boolean>
    );
  }

  syncAll(connectorName: string, request: SyncAllRequest = {}): Observable<SyncAccepted> {
    return this.api.post<SyncAccepted>(`core/connectors/${this.pathSegment(connectorName)}/sync-all`, request);
  }

  getSyncJob(jobId: string): Observable<SyncJob> {
    return this.api.get<SyncJob>(`core/connectors/sync-jobs/${this.pathSegment(jobId)}`);
  }

  private pathSegment(value: string): string {
    return encodeURIComponent(value);
  }
}
