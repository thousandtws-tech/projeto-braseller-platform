import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  AccountingPeriodClosing,
  AccountingPeriodSignatureRequest,
  AvailableFilters,
  CloudinaryUploadSignature,
  DashboardView,
  DreCalculationJob,
  DreCalculationRequest,
  DreStatement,
  ExpenseEntry,
  ExpensePage,
  ExpenseRequest,
  FinancialSummary,
  FiscalProfile,
  FiscalProfileRequest,
  MonthlyEvolutionPoint,
  PaymentReleaseAlert,
  PlatformComparisonPoint,
  PublicReportEntryImportRequest,
  ReportEntryPage,
  ReportExportFormat,
  ReportFilters,
} from '../models/reporting.model';

/**
 * ReportingService -> reporting-service via gateway.
 * Base real: {environment.apiUrl}/reports...
 */
@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly api = inject(ApiService);

  getStatus(): Observable<{ status?: string; service?: string; message?: string } | string> {
    return this.api.get<{ status?: string; service?: string; message?: string } | string>('reports');
  }

  getDashboard(tenantId: string, filters: ReportFilters = {}): Observable<DashboardView> {
    return this.api.get<DashboardView>(
      `reports/tenants/${this.pathSegment(tenantId)}/dashboard`,
      this.query(filters)
    );
  }

  getSummary(tenantId: string, filters: ReportFilters = {}): Observable<FinancialSummary> {
    return this.api.get<FinancialSummary>(
      `reports/tenants/${this.pathSegment(tenantId)}/summary`,
      this.query(filters)
    );
  }

  getInternalSummary(tenantId: string): Observable<FinancialSummary> {
    return this.api.get<FinancialSummary>(`reports/internal/tenants/${this.pathSegment(tenantId)}/summary`);
  }

  getEntries(tenantId: string, filters: ReportFilters = {}): Observable<ReportEntryPage> {
    return this.api.get<ReportEntryPage>(
      `reports/tenants/${this.pathSegment(tenantId)}/entries`,
      this.query(filters)
    );
  }

  getAvailableFilters(tenantId: string): Observable<AvailableFilters> {
    return this.api.get<AvailableFilters>(`reports/tenants/${this.pathSegment(tenantId)}/filters`);
  }

  importManualEntry(tenantId: string, request: PublicReportEntryImportRequest): Observable<unknown> {
    return this.api.post<unknown>(
      `reports/tenants/${this.pathSegment(tenantId)}/manual-import/entries`,
      request
    );
  }

  importIntegrationEntry(tenantId: string, request: PublicReportEntryImportRequest): Observable<unknown> {
    return this.api.post<unknown>(
      `reports/tenants/${this.pathSegment(tenantId)}/integrations/entries`,
      request
    );
  }

  ingestInternalEntry(request: PublicReportEntryImportRequest & { tenant_id: string }): Observable<unknown> {
    return this.api.post<unknown>('reports/internal/entries', request);
  }

  getMonthlyEvolution(
    tenantId: string,
    filters: Pick<ReportFilters, 'from' | 'to' | 'platform' | 'paymentMethod' | 'status'> = {}
  ): Observable<MonthlyEvolutionPoint[]> {
    return this.api.get<MonthlyEvolutionPoint[]>(
      `reports/tenants/${this.pathSegment(tenantId)}/charts/monthly-evolution`,
      this.query(filters)
    );
  }

  getPlatformComparison(
    tenantId: string,
    filters: Pick<ReportFilters, 'from' | 'to' | 'paymentMethod' | 'status'> = {}
  ): Observable<PlatformComparisonPoint[]> {
    return this.api.get<PlatformComparisonPoint[]>(
      `reports/tenants/${this.pathSegment(tenantId)}/charts/platform-comparison`,
      this.query(filters)
    );
  }

  getPaymentReleases(tenantId: string): Observable<PaymentReleaseAlert[]> {
    return this.api.get<PaymentReleaseAlert[]>(
      `reports/internal/tenants/${this.pathSegment(tenantId)}/payment-releases`
    );
  }

  getFiscalProfile(tenantId: string): Observable<FiscalProfile> {
    return this.api.get<FiscalProfile>(`reports/tenants/${this.pathSegment(tenantId)}/fiscal-profile`);
  }

  saveFiscalProfile(tenantId: string, request: FiscalProfileRequest): Observable<FiscalProfile> {
    return this.api.put<FiscalProfile>(
      `reports/tenants/${this.pathSegment(tenantId)}/fiscal-profile`,
      request
    );
  }

  getExpenses(
    tenantId: string,
    params: { from?: string; to?: string; category?: string; page?: number; size?: number } = {}
  ): Observable<ExpensePage> {
    return this.api.get<ExpensePage>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses`,
      this.query(params)
    );
  }

  getExpense(tenantId: string, expenseId: string): Observable<ExpenseEntry> {
    return this.api.get<ExpenseEntry>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses/${this.pathSegment(expenseId)}`
    );
  }

  createExpense(tenantId: string, request: ExpenseRequest): Observable<ExpenseEntry> {
    return this.api.post<ExpenseEntry>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses`,
      request
    );
  }

  updateExpense(tenantId: string, expenseId: string, request: ExpenseRequest): Observable<ExpenseEntry> {
    return this.api.put<ExpenseEntry>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses/${this.pathSegment(expenseId)}`,
      request
    );
  }

  deleteExpense(tenantId: string, expenseId: string): Observable<unknown> {
    return this.api.delete<unknown>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses/${this.pathSegment(expenseId)}`
    );
  }

  getUploadSignature(tenantId: string): Observable<CloudinaryUploadSignature> {
    return this.api.get<CloudinaryUploadSignature>(
      `reports/tenants/${this.pathSegment(tenantId)}/expenses/upload-signature`
    );
  }

  getDre(tenantId: string, from: string, to: string): Observable<DreStatement> {
    return this.api.get<DreStatement>(
      `reports/tenants/${this.pathSegment(tenantId)}/dre`,
      { from, to }
    );
  }

  enqueueDreCalculation(
    tenantId: string,
    requestOrFrom: DreCalculationRequest | string,
    to?: string
  ): Observable<DreCalculationJob | unknown> {
    const request: DreCalculationRequest =
      typeof requestOrFrom === 'string' ? { from: requestOrFrom, to: to ?? requestOrFrom } : requestOrFrom;
    return this.api.post<DreCalculationJob | unknown>(
      `reports/tenants/${this.pathSegment(tenantId)}/dre/jobs`,
      request
    );
  }

  getDreJob(tenantId: string, jobId: string): Observable<DreCalculationJob> {
    return this.api.get<DreCalculationJob>(
      `reports/tenants/${this.pathSegment(tenantId)}/dre/jobs/${this.pathSegment(jobId)}`
    );
  }

  getMonthlyClosing(tenantId: string, month: string): Observable<AccountingPeriodClosing> {
    return this.api.get<AccountingPeriodClosing>(
      `reports/tenants/${this.pathSegment(tenantId)}/closings/${this.pathSegment(month)}`
    );
  }

  signMonthlyClosing(
    tenantId: string,
    month: string,
    request: AccountingPeriodSignatureRequest
  ): Observable<AccountingPeriodClosing> {
    return this.api.post<AccountingPeriodClosing>(
      `reports/tenants/${this.pathSegment(tenantId)}/closings/${this.pathSegment(month)}/sign`,
      request
    );
  }

  downloadMonthlyExport(tenantId: string, month: string, format: ReportExportFormat): Observable<Blob> {
    return this.api.getBlob(
      `reports/tenants/${this.pathSegment(tenantId)}/exports/monthly`,
      { month, format }
    );
  }

  downloadPlatformExport(
    tenantId: string,
    platform: string,
    from: string,
    to: string,
    format: ReportExportFormat
  ): Observable<Blob> {
    return this.api.getBlob(
      `reports/tenants/${this.pathSegment(tenantId)}/exports/platforms/${this.pathSegment(platform)}`,
      { from, to, format }
    );
  }

  buildExportMonthlyUrl(tenantId: string, month: string, format: ReportExportFormat): string {
    return `reports/tenants/${this.pathSegment(tenantId)}/exports/monthly?month=${encodeURIComponent(month)}&format=${encodeURIComponent(format)}`;
  }

  buildExportPlatformUrl(
    tenantId: string,
    platform: string,
    from: string,
    to: string,
    format: ReportExportFormat
  ): string {
    return `reports/tenants/${this.pathSegment(tenantId)}/exports/platforms/${this.pathSegment(platform)}?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&format=${encodeURIComponent(format)}`;
  }

  private query(params: object): Record<string, string | number | boolean> {
    return Object.entries(params).reduce<Record<string, string | number | boolean>>((current, [key, value]) => {
      if (value === undefined || value === null || value === '') return current;
      current[key] = value as string | number | boolean;
      return current;
    }, {});
  }

  private pathSegment(value: string): string {
    return encodeURIComponent(value);
  }
}
