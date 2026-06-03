// Models mapeados diretamente do notification-service (Quarkus)
// Base: GET /notifications/tenants/{tenantId}, PATCH /notifications/tenants/{tenantId}/{id}/read

export interface NotificationMessage {
  id: string;
  tenantId: string;
  type: string;        // 'NEW_SALE', 'ML_PAYMENT_RELEASE', 'MONTHLY_CLOSING', 'WEEKLY_ACCOUNTANT_REPORT'
  subject: string;     // título da notificação
  body: string;        // corpo / mensagem
  read: boolean;
  archivedAt: string | null;
  createdAt: string;
}

export interface NotificationPreference {
  tenantId: string;
  emailEnabled: boolean;
  newSaleEnabled: boolean;
  monthlyClosingEnabled: boolean;
  mlPaymentReleaseEnabled: boolean;
  weeklyAccountantReportEnabled: boolean;
  recipientEmail: string;
  accountantEmail: string | null;
}

export interface PreferenceRequest {
  emailEnabled: boolean;
  newSaleEnabled: boolean;
  monthlyClosingEnabled: boolean;
  mlPaymentReleaseEnabled: boolean;
  weeklyAccountantReportEnabled: boolean;
  recipientEmail: string;
  accountantEmail?: string;
}

export interface ClearReadResponse {
  archivedCount: number;
}

export interface TenantNewSaleSummary {
  tenantId: string;
  totalCount: number;
  totalAmount: number;
  lastSaleDate: string | null;
}
