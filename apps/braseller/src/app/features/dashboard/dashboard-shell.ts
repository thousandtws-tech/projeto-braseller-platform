import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, filter as rxFilter, finalize, map, switchMap } from 'rxjs/operators';
import { UserView } from '../../core/models/user.model';
import {
  ConnectorConnectionStatus,
  ConnectorDescriptor,
  ConnectorStatus,
  StandardOrder,
} from '../../core/models/connector.model';
import { NotificationMessage } from '../../core/models/notification.model';
import {
  AvailableFilters,
  DashboardView,
  DreCalculationJob,
  DreStatement,
  ExpenseCategory,
  ExpenseEntry,
  ExpenseRequest,
  FinancialSummary,
  MonthlyEvolutionPoint,
  PlatformComparisonPoint,
  PublicReportEntryImportRequest,
  ReportEntry,
  ReportEntryPage,
  ReportEntryStatus,
} from '../../core/models/reporting.model';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { CoreService } from '../../core/services/core.service';
import { NotificationService } from '../../core/services/notification.service';
import { ReportingService } from '../../core/services/reporting.service';

// â”€â”€â”€ Interfaces locais (anti-corruption layer entre backend e template) â”€â”€â”€â”€â”€â”€â”€

interface Tenant {
  id: string;
  name: string;
  sellerName: string;
  email: string;
  role: 'seller' | 'accountant' | 'seller_sec';
  plan: string;
  trialDaysLeft: number;
  mlConnected: boolean;
  shopeeConnected: boolean;
  amazonConnected: boolean;
}

interface Connector {
  key: string;
  name: string;
  active: boolean;
  version: string;
  description: string;
  type: string;
  status: ConnectorConnectionStatus;
  statusMessage: string;
  checkedAt: string | null;
  supportsInvoices: boolean;
  requiredMethods: string[];
  optionalMethods: string[];
  syncJobId?: string;
}

interface OrderItem {
  name: string;
  qty: number;
  unit_price: number;
}

interface Order {
  order_id: string;
  platform: string;
  date: string;
  gross_value: number;
  platform_fee: number;
  net_value: number;
  payment_method: string;
  payment_date: string;
  release_date: string;
  status: 'paid' | 'pending' | 'cancelled';
  buyer_name: string;
  items: OrderItem[];
  invoice_number: string;
}

interface DashboardSummary {
  gross_value: number;
  platform_fee: number;
  net_value: number;
  pending_value: number;
  total_orders: number;
  paid_orders_count: number;
  average_ticket: number;
  platform_split: Record<string, number>;
  currency: string;
  active_integrations: string[];
}

interface PlatformSplitEntry {
  key: string;
  name: string;
  value: number;
  width: string;
  colorClass: string;
}

interface AppNotification {
  id: string;
  title: string;
  message: string;
  type: 'success' | 'info' | 'warning' | 'danger';
  date: string;
  read: boolean;
}

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  date: Date;
  isReal?: boolean;
}

export type DashboardTab = 'dashboard' | 'connectors' | 'orders' | 'expenses' | 'dre' | 'chat' | 'profile';

// â”€â”€â”€ Mappers (ReportEntry/NotificationMessage â†’ interfaces locais do template) â”€

function mapReportEntryStatus(status: ReportEntryStatus): 'paid' | 'pending' | 'cancelled' {
  switch (status) {
    case 'PAID':
    case 'RECEIVED':
      return 'paid';
    case 'PENDING_RELEASE':
      return 'pending';
    default:
      return 'cancelled';
  }
}

function mapPlatformKey(platform: string): string {
  const p = platform.toLowerCase().trim();
  const normalized = p.replace(/[\s_]+/g, '-');
  if (normalized === 'ml' || normalized === 'mercadolivre' || normalized === 'mercado-livre') return 'mercado-livre';
  if (p === 'shopee') return 'shopee';
  if (p === 'amazon') return 'amazon';
  if (p === 'sandbox') return 'sandbox';
  return 'manual';
}

function mapPaymentMethodDisplay(method: string): string {
  const map: Record<string, string> = {
    PIX: 'PIX', CREDIT_CARD: 'CartÃ£o de CrÃ©dito', DEBIT_CARD: 'CartÃ£o de DÃ©bito',
    BANK_TRANSFER: 'TransferÃªncia', BOLETO: 'Boleto', WALLET: 'Carteira Digital', OTHER: 'Outro',
    CARD: 'Cartão', MARKETPLACE_BALANCE: 'Saldo Marketplace', UNKNOWN: 'Nao informado',
  };
  return map[method] ?? method;
}

function mapNotificationType(type: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (type) {
    case 'NEW_SALE':                    return 'success';
    case 'ML_PAYMENT_RELEASE':          return 'info';
    case 'MONTHLY_CLOSING':             return 'info';
    case 'WEEKLY_ACCOUNTANT_REPORT':    return 'info';
    default:                            return 'info';
  }
}

// â”€â”€â”€ Componente â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, RouterOutlet],
  providers: [DecimalPipe, DatePipe],
  templateUrl: './dashboard-shell.html',
})
export class DashboardShell implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private coreService = inject(CoreService);
  private notificationService = inject(NotificationService);
  private reportingService = inject(ReportingService);
  private router = inject(Router);

  // â”€â”€â”€ Estado da UI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  readonly Math = Math;
  tenant = signal<Tenant | null>(null);
  members = signal<UserView[]>([]);
  connectors = signal<Connector[]>([]);
  summary = signal<DashboardSummary | null>(null);
  orders = signal<Order[]>([]);
  availableFilters = signal<AvailableFilters | null>(null);
  monthlyEvolution = signal<MonthlyEvolutionPoint[]>([]);
  platformComparison = signal<PlatformComparisonPoint[]>([]);
  notifications = signal<AppNotification[]>([]);
  selectedOrder = signal<Order | null>(null);
  isOrderModalOpen = signal<boolean>(false);
  showNotificationsMenu = signal<boolean>(false);

  // Estados de carregamento individuais
  isLoadingOrders = signal<boolean>(false);
  isLoadingConnectors = signal<boolean>(false);
  isLoadingSummary = signal<boolean>(false);
  isLoadingNotifications = signal<boolean>(false);

  // â”€â”€â”€ Chat â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  chatMessages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: '### BraSeller IA\n\nA autenticacao e o perfil de usuario agora usam o gateway real. A assistencia analitica sera ligada ao backend em uma proxima etapa.',
      date: new Date(),
      isReal: true,
    }
  ]);
  chatLoading = signal<boolean>(false);

  suggestions = [
    'Qual plataforma possui as maiores taxas e qual sua receita lÃ­quida?',
    'Fazer fechamento para meu contador',
    'PrevisÃ£o de faturamento para o prÃ³ximo mÃªs'
  ];

  // â”€â”€â”€ FormulÃ¡rios â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  filterForm!: FormGroup;
  chatForm!: FormGroup;
  manualForm!: FormGroup;

  activeTab = signal<DashboardTab>('dashboard');
  isSyncing = signal<boolean>(false);
  isManualModalOpen = signal<boolean>(false);

  // Expenses
  expenses = signal<ExpenseEntry[]>([]);
  isLoadingExpenses = signal<boolean>(false);
  isExpenseModalOpen = signal<boolean>(false);
  editingExpense = signal<ExpenseEntry | null>(null);
  expenseForm!: FormGroup;

  // DRE
  dreStatement = signal<DreStatement | null>(null);
  dreJob = signal<DreCalculationJob | null>(null);
  isLoadingDre = signal<boolean>(false);
  dreForm!: FormGroup;

  // â”€â”€â”€ Ciclo de vida â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  ngOnInit() {
    this.initForms();
    this.bindRouteState();
    this.loadAllData();
  }

  private initForms() {
    this.filterForm = this.fb.group({
      from: [''],
      to: [''],
      platform: ['all'],
      paymentMethod: ['all'],
      status: ['all'],
      search: [''],
    });

    this.expenseForm = this.fb.group({
      expense_date: [new Date().toISOString().split('T')[0], [Validators.required]],
      category: ['OUTRAS', [Validators.required]],
      description: ['', [Validators.required, Validators.minLength(3)]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
    });

    const now = new Date();
    this.dreForm = this.fb.group({
      from: [new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0], [Validators.required]],
      to:   [new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0], [Validators.required]],
    });

    this.chatForm = this.fb.group({
      message: ['', [Validators.required]],
    });

    this.manualForm = this.fb.group({
      buyer_name: ['', [Validators.required, Validators.minLength(2)]],
      item_name: ['', [Validators.required]],
      qty: [1, [Validators.required, Validators.min(1)]],
      unit_price: [0, [Validators.required, Validators.min(0.01)]],
      platform_fee: [0, [Validators.required, Validators.min(0)]],
      payment_method: ['PIX', [Validators.required]],
      status: ['paid', [Validators.required]],
      invoice_number: ['']
    });

    this.filterForm.valueChanges.subscribe(() => this.loadReportingDashboard());
  }

  // â”€â”€â”€ Carga de dados â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  loadAllData() {
    this.loadTenant();
  }

  navigateTo(tab: DashboardTab) {
    this.activeTab.set(tab);
    void this.router.navigate(this.routeForTab(tab));
    if (tab === 'expenses') this.loadExpenses();
  }

  private bindRouteState() {
    this.setActiveTabFromUrl(this.router.url);
    this.router.events.pipe(
      rxFilter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe(event => this.setActiveTabFromUrl(event.urlAfterRedirects));
  }

  private setActiveTabFromUrl(url: string) {
    const cleanUrl = url.split('?')[0].split('#')[0];
    const segment = cleanUrl.split('/').filter(Boolean)[1] ?? '';
    const tab = this.tabFromRouteSegment(segment);
    this.activeTab.set(tab);
    if (tab === 'expenses') this.loadExpenses();
  }

  private routeForTab(tab: DashboardTab): string[] {
    if (tab === 'dashboard') return ['/dashboard'];
    if (tab === 'chat') return ['/dashboard', 'ai'];
    return ['/dashboard', tab];
  }

  private tabFromRouteSegment(segment: string): DashboardTab {
    if (segment === 'orders') return 'orders';
    if (segment === 'expenses') return 'expenses';
    if (segment === 'dre') return 'dre';
    if (segment === 'connectors') return 'connectors';
    if (segment === 'ai') return 'chat';
    if (segment === 'profile') return 'profile';
    return 'dashboard';
  }

  loadTenant() {
    const session = this.authService.restoreSession();
    if (session) {
      this.tenant.set(session.tenant);
      void this.loadUserMembers(session.tenant.id);
      this.loadConnectors();
      this.loadReportingDashboard();
      this.loadNotifications();
      return;
    }
    this.router.navigate(['/login']);
  }

  /** GET /core/connectors + GET /core/connectors/{name}/status */
  loadConnectors() {
    this.isLoadingConnectors.set(true);
    this.coreService.getConnectors().pipe(
      catchError(() => of([] as ConnectorDescriptor[])),
      map(descriptors => descriptors.filter(descriptor => descriptor.name.toLowerCase() !== 'sandbox')),
      switchMap(descriptors => {
        if (!descriptors.length) {
          return of({ descriptors, statuses: [] as ConnectorStatus[] });
        }

        const statusCalls = descriptors.map(descriptor =>
          this.coreService.getConnectorStatus(descriptor.name).pipe(
            catchError(() => of(this.unavailableConnectorStatus(descriptor.name)))
          )
        );

        return forkJoin(statusCalls).pipe(
          map(statuses => ({ descriptors, statuses }))
        );
      }),
      finalize(() => this.isLoadingConnectors.set(false))
    ).subscribe(({ descriptors, statuses }) => {
      const statusMap = new Map(statuses.map(status => [status.platform.toLowerCase(), status]));
      const connectors = descriptors.map(descriptor =>
        this.adaptConnector(descriptor, statusMap.get(descriptor.name.toLowerCase()))
      );
      this.connectors.set(connectors);
      this.updateTenantConnectorFlags(connectors);
      this.updateSummaryOrderMetrics();
    });
  }

  /** GET /reports/tenants/{id}/dashboard — cards, tabela, graficos e filtros */
  loadReportingDashboard() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    const filters = this.currentReportFilters();
    this.isLoadingOrders.set(true);
    this.isLoadingSummary.set(true);

    this.reportingService.getDashboard(tenantId, { ...filters, size: 100 }).pipe(
      catchError(() => of(null as DashboardView | null)),
      finalize(() => {
        this.isLoadingOrders.set(false);
        this.isLoadingSummary.set(false);
      })
    ).subscribe(view => {
      if (!view) {
        this.loadSummary();
        this.loadOrders();
        return;
      }

      this.availableFilters.set(view.filters ?? null);
      this.monthlyEvolution.set(view.monthly_evolution ?? []);
      this.platformComparison.set(view.platform_comparison ?? []);
      this.summary.set(this.adaptFinancialSummary(view.summary, view.platform_comparison ?? []));

      const reportOrders = (view.entries?.items ?? []).map(entry => this.adaptReportEntry(entry));
      if (reportOrders.length > 0) {
        this.setOrders(reportOrders);
        return;
      }
      this.loadCoreConnectorOrders(filters);
    });
  }

  /** GET /reports/tenants/{id}/entries â€” lanÃ§amentos com filtros */
  loadOrders() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    const filters = this.currentReportFilters();

    this.isLoadingOrders.set(true);
    this.reportingService.getEntries(tenantId, { ...filters, size: 100 }).pipe(
      catchError(() => of({ items: [], total: 0, page: 0, size: 0 } as ReportEntryPage)),
      finalize(() => this.isLoadingOrders.set(false))
    ).subscribe(page => {
      const reportOrders = (page.items ?? []).map(entry => this.adaptReportEntry(entry));
      if (reportOrders.length > 0) {
        this.setOrders(reportOrders);
        return;
      }
      this.loadCoreConnectorOrders(filters);
    });
  }

  /** GET /reports/tenants/{id}/summary â€” cards financeiros do painel */
  loadSummary() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    this.isLoadingSummary.set(true);
    this.reportingService.getSummary(tenantId).pipe(
      catchError(() => of(null)),
      finalize(() => this.isLoadingSummary.set(false))
    ).subscribe(fs => {
      if (fs) {
        this.summary.set(this.adaptFinancialSummary(fs));
      }
    });
  }

  private currentReportFilters(): Record<string, string> {
    const { platform, status, search, from, to, paymentMethod } = this.filterForm?.value ?? {};
    const filters: Record<string, string> = {};
    if (platform && platform !== 'all') filters['platform'] = platform;
    if (status && status !== 'all') filters['status'] = status.toUpperCase();
    if (search?.trim()) filters['search'] = search.trim();
    if (from) filters['from'] = from;
    if (to) filters['to'] = to;
    if (paymentMethod && paymentMethod !== 'all') filters['paymentMethod'] = paymentMethod;
    return filters;
  }

  /** GET /notifications/tenants/{id} â€” notificaÃ§Ãµes do tenant */
  loadNotifications() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    this.isLoadingNotifications.set(true);
    this.notificationService.getNotifications(tenantId, 50).pipe(
      catchError(() => of([] as NotificationMessage[])),
      finalize(() => this.isLoadingNotifications.set(false))
    ).subscribe(messages => {
      this.notifications.set((messages ?? []).map(m => this.adaptNotification(m)));
    });
  }

  private async loadUserMembers(tenantId: string): Promise<void> {
    try {
      const { firstValueFrom } = await import('rxjs');
      const members = await firstValueFrom(this.userService.listTenantMembers(tenantId));
      this.members.set(Array.isArray(members) ? members : []);
    } catch {
      this.members.set([]);
    }
  }

  /** GET /core/connectors/{name}/orders â€” fallback enquanto reporting ainda nÃ£o recebeu lanÃ§amentos */
  private loadCoreConnectorOrders(filters: Record<string, string>) {
    const selectedPlatform = filters['platform'] ? mapPlatformKey(filters['platform']) : null;
    const activeConnectors = this.connectors()
      .filter(c => c.active)
      .filter(c => !selectedPlatform || c.key === selectedPlatform)
      .map(c => c.key);
    if (activeConnectors.length === 0) {
      this.setOrders([]);
      return;
    }

    const status = filters['status'] ? filters['status'].toLowerCase() : undefined;
    const calls = activeConnectors.map(connectorName =>
      this.coreService.getConnectorOrders(connectorName, { status, limit: 100 }).pipe(
        catchError(() => of([] as StandardOrder[]))
      )
    );

    this.isLoadingOrders.set(true);
    forkJoin(calls).pipe(
      finalize(() => this.isLoadingOrders.set(false))
    ).subscribe(results => {
      this.setOrders(results.flat().map(order => this.adaptStandardOrder(order)));
    });
  }

  // â”€â”€â”€ AÃ§Ãµes de notificaÃ§Ã£o â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  /** PATCH /notifications/tenants/{tenantId}/{id}/read */
  markAsRead(id: string) {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    // Otimista: atualiza imediatamente
    this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));

    this.notificationService.markAsRead(tenantId, id).pipe(
      catchError(() => of(null))
    ).subscribe();
  }

  /** POST /notifications/tenants/{tenantId}/clear-read â€” arquiva lidas */
  clearNotifications() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) {
      this.notifications.set([]);
      return;
    }

    this.notificationService.clearReadNotifications(tenantId).pipe(
      catchError(() => of(null))
    ).subscribe(() => {
      // Remove notificaÃ§Ãµes lidas localmente apÃ³s arquivar no backend
      this.notifications.update(list => list.filter(n => !n.read));
    });
  }

  // â”€â”€â”€ AÃ§Ãµes de sincronizaÃ§Ã£o â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  /** POST /core/connectors/{name}/sync-all — sincroniza todos os conectores ativos */
  syncAll() {
    const activeConnectors = this.connectors().filter(c => c.active).map(c => c.key);
    if (activeConnectors.length === 0) {
      this.addLocalToastNotification(
        'Nenhum conector ativo',
        'Ative ao menos um conector de marketplace para sincronizar vendas.',
        'warning'
      );
      return;
    }

    this.isSyncing.set(true);

    const syncCalls = activeConnectors.map(name =>
      this.coreService.syncAll(name).pipe(catchError(() => of(null)))
    );

    forkJoin(syncCalls).pipe(
      finalize(() => this.isSyncing.set(false))
    ).subscribe(results => {
      const succeeded = results.filter(r => r !== null).length;
      const jobIds = results
        .map(result => result?.job_id)
        .filter((jobId): jobId is string => Boolean(jobId));

      this.addLocalToastNotification(
        'Sincronizacao enfileirada no Core',
        jobIds.length
          ? `${succeeded} de ${activeConnectors.length} jobs aceitos: ${jobIds.join(', ')}.`
          : `${succeeded} de ${activeConnectors.length} conectores foram aceitos pelo core-service.`,
        succeeded > 0 ? 'success' : 'warning'
      );
      if (succeeded > 0) {
        setTimeout(() => { this.loadConnectors(); this.loadOrders(); this.loadSummary(); }, 2000);
      }
    });
  }
  // Acoes de conectores â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  toggleConnector(key: string, currentStatus: boolean) {
    if (!currentStatus) {
      this.addLocalToastNotification(
        `Conector ${this.getPlatformName(key)} - configuracao pendente`,
        'A autenticacao OAuth do conector deve usar /core/connectors/{name}/authenticate.',
        'warning'
      );
      return;
    }

    this.isSyncing.set(true);
    this.coreService.syncAll(key).pipe(
      catchError(() => {
        this.addLocalToastNotification(
          `Falha ao sincronizar ${this.getPlatformName(key)}`,
          'O core-service nao aceitou o job de sincronizacao para este conector.',
          'danger'
        );
        return of(null);
      }),
      finalize(() => this.isSyncing.set(false))
    ).subscribe(result => {
      if (!result) return;

      this.connectors.update(items => items.map(conn =>
        conn.key === key ? { ...conn, syncJobId: result.job_id } : conn
      ));
      this.addLocalToastNotification(
        `Sync ${this.getPlatformName(key)} enfileirado`,
        `Job ${result.job_id} aceito pelo core-service.`,
        'success'
      );
      setTimeout(() => { this.loadOrders(); this.loadSummary(); }, 2000);
    });
  }
  // Lancamento manual â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  openManualOrderModal() {
    this.manualForm.reset({
      buyer_name: '', item_name: '', qty: 1, unit_price: 0,
      platform_fee: 0, payment_method: 'PIX', status: 'paid', invoice_number: ''
    });
    this.isManualModalOpen.set(true);
  }

  /** POST /reports/tenants/{id}/manual-import/entries */
  submitManualOrder() {
    if (this.manualForm.invalid) return;
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    const f = this.manualForm.getRawValue();
    const grossValue = Number(f.qty) * Number(f.unit_price);
    const feeValue = Number(f.platform_fee);
    const receivedValue = grossValue - feeValue;

    const request: PublicReportEntryImportRequest = {
      platform: 'manual',
      order_id: 'manual_' + Date.now(),
      sale_date: new Date().toISOString().split('T')[0],
      gross_value: grossValue,
      fee_value: feeValue,
      received_value: receivedValue,
      receivable_value: receivedValue,
      payment_method: f.payment_method,
      status: f.status.toUpperCase(),
      buyer_name: f.buyer_name,
      invoice_number: f.invoice_number || undefined,
    };

    this.reportingService.importManualEntry(tenantId, request).pipe(
      catchError(() => {
        this.addLocalToastNotification('Erro ao salvar', 'NÃ£o foi possÃ­vel criar o lanÃ§amento manual.', 'danger');
        return of(null);
      })
    ).subscribe(entry => {
      if (entry) {
        this.isManualModalOpen.set(false);
        this.addLocalToastNotification('LanÃ§amento criado', 'O lanÃ§amento manual foi registrado com sucesso.', 'success');
        this.loadReportingDashboard();
      }
    });
  }

  // â”€â”€â”€ Despesas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  loadExpenses() {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;
    const { from, to } = this.filterForm?.value ?? {};
    this.isLoadingExpenses.set(true);
    this.reportingService.getExpenses(tenantId, {
      from: from || undefined,
      to: to || undefined,
      page: 0,
      size: 100,
    }).pipe(
      catchError(() => of({ items: [], total: 0, page: 0, size: 0 })),
      finalize(() => this.isLoadingExpenses.set(false))
    ).subscribe(page => {
      this.expenses.set(page.items ?? []);
    });
  }

  openCreateExpense() {
    this.editingExpense.set(null);
    this.expenseForm.reset({
      expense_date: new Date().toISOString().split('T')[0],
      category: 'OUTRAS',
      description: '',
      amount: 0,
    });
    this.isExpenseModalOpen.set(true);
  }

  openEditExpense(expense: ExpenseEntry) {
    this.editingExpense.set(expense);
    this.expenseForm.patchValue({
      expense_date: expense.expense_date,
      category: expense.category,
      description: expense.description,
      amount: expense.amount,
    });
    this.isExpenseModalOpen.set(true);
  }

  saveExpense() {
    if (this.expenseForm.invalid) return;
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;

    const request: ExpenseRequest = {
      expense_date: this.expenseForm.value.expense_date,
      category: this.expenseForm.value.category,
      description: this.expenseForm.value.description,
      amount: Number(this.expenseForm.value.amount),
    };

    const editing = this.editingExpense();
    const call$ = editing
      ? this.reportingService.updateExpense(tenantId, editing.id, request)
      : this.reportingService.createExpense(tenantId, request);

    call$.pipe(
      catchError(() => {
        this.addLocalToastNotification('Erro', 'Nao foi possivel salvar a despesa.', 'danger');
        return of(null);
      })
    ).subscribe(result => {
      if (result) {
        this.isExpenseModalOpen.set(false);
        this.addLocalToastNotification(
          'Despesa salva',
          editing ? 'Despesa atualizada com sucesso.' : 'Despesa registrada com sucesso.',
          'success'
        );
        this.loadExpenses();
      }
    });
  }

  deleteExpense(id: string) {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;
    this.reportingService.deleteExpense(tenantId, id).pipe(
      catchError(() => {
        this.addLocalToastNotification('Erro', 'Nao foi possivel excluir a despesa.', 'danger');
        return of(null);
      })
    ).subscribe(() => {
      this.expenses.update(list => list.filter(e => e.id !== id));
      this.addLocalToastNotification('Despesa removida', 'Despesa excluida com sucesso.', 'success');
    });
  }

  getExpensesSum(): number {
    return this.expenses().reduce((sum, e) => sum + Number(e.amount ?? 0), 0);
  }

  getExpenseCategoryLabel(category: string): string {
    const labels: Record<string, string> = {
      ALUGUEL: 'Aluguel', ENERGIA: 'Energia Eletrica', AGUA: 'Agua', INTERNET: 'Internet',
      TELEFONE: 'Telefone', MANUTENCAO: 'Manutencao', MATERIAL: 'Material de Escritorio',
      SERVICOS: 'Servicos', TRANSPORTES: 'Transportes', OUTRAS: 'Outras',
    };
    return labels[category] ?? category;
  }

  getExpenseCategoryOptions(): ExpenseCategory[] {
    return ['ALUGUEL', 'ENERGIA', 'AGUA', 'INTERNET', 'TELEFONE', 'MANUTENCAO', 'MATERIAL', 'SERVICOS', 'TRANSPORTES', 'OUTRAS'];
  }

  getPaymentMethodOptions(): { key: string; label: string }[] {
    return [
      { key: 'PIX', label: 'PIX' },
      { key: 'CREDIT_CARD', label: 'Cartao de Credito' },
      { key: 'DEBIT_CARD', label: 'Cartao de Debito' },
      { key: 'BOLETO', label: 'Boleto' },
      { key: 'BANK_TRANSFER', label: 'Transferencia' },
      { key: 'MARKETPLACE_BALANCE', label: 'Saldo Marketplace' },
      { key: 'OTHER', label: 'Outro' },
    ];
  }

  // â”€â”€â”€ DRE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  loadDre() {
    const tenantId = this.tenant()?.id;
    if (!tenantId || this.dreForm.invalid) return;
    const { from, to } = this.dreForm.value;
    this.isLoadingDre.set(true);
    this.reportingService.getDre(tenantId, from, to).pipe(
      catchError(() => {
        this.addLocalToastNotification('DRE indisponivel', 'Calcule o DRE primeiro ou aguarde o processamento.', 'warning');
        return of(null);
      }),
      finalize(() => this.isLoadingDre.set(false))
    ).subscribe(statement => {
      this.dreStatement.set(statement);
    });
  }

  enqueueDre() {
    const tenantId = this.tenant()?.id;
    if (!tenantId || this.dreForm.invalid) return;
    const { from, to } = this.dreForm.value;
    this.isLoadingDre.set(true);
    this.reportingService.enqueueDreCalculation(tenantId, from, to).pipe(
      catchError(() => {
        this.addLocalToastNotification('Erro ao calcular DRE', 'O servico de calculo nao esta disponivel.', 'danger');
        return of(null);
      }),
      finalize(() => this.isLoadingDre.set(false))
    ).subscribe(job => {
      if (job) {
        this.dreJob.set(job as DreCalculationJob);
        this.addLocalToastNotification('DRE enfileirado', 'Calculo iniciado. Aguarde e recarregue em alguns segundos.', 'info');
        setTimeout(() => this.loadDre(), 3000);
      }
    });
  }

  getDreStatusLabel(status: string): string {
    return { QUEUED: 'Na fila', PROCESSING: 'Processando', COMPLETED: 'Concluido', FAILED: 'Com erro' }[status] ?? status;
  }

  getDreTaxRegimeLabel(regime: string): string {
    return { SIMPLES_NACIONAL: 'Simples Nacional', LUCRO_PRESUMIDO: 'Lucro Presumido', LUCRO_REAL: 'Lucro Real' }[regime] ?? regime;
  }

  // â”€â”€â”€ Demais aÃ§Ãµes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  openOrderDetails(order: Order) {
    this.selectedOrder.set(order);
    this.isOrderModalOpen.set(true);
  }

  closeOrderDetails() {
    this.isOrderModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  clearFilters() {
    this.filterForm.patchValue({ from: '', to: '', platform: 'all', paymentMethod: 'all', status: 'all', search: '' });
  }

  async logout() {
    try {
      const { firstValueFrom } = await import('rxjs');
      await firstValueFrom(this.authService.logout());
    } catch { /* ignora erros de logout */ }
    this.clearLocalSessionAndRedirect();
  }

  clearLocalSessionAndRedirect() {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }

  // â”€â”€â”€ ExportaÃ§Ã£o client-side (usa dados jÃ¡ carregados do reporting-service) â”€â”€

  exportCSV() {
    this.downloadMonthlyExport('CSV', `braseller_export_${this.currentMonth()}.csv`);
  }

  exportExcel() {
    this.downloadMonthlyExport('XLSX', `braseller_conciliacao_${this.currentMonth()}.xlsx`);
  }

  exportPDFContador() {
    this.downloadMonthlyExport('PDF', `braseller_contabil_${this.currentMonth()}.pdf`);
  }

  private downloadMonthlyExport(format: 'PDF' | 'XLSX' | 'CSV', filename: string) {
    const tenantId = this.tenant()?.id;
    if (!tenantId) return;
    this.reportingService.downloadMonthlyExport(tenantId, this.currentMonth(), format).pipe(
      catchError(() => {
        this.addLocalToastNotification('Exportacao indisponivel', 'O reporting-service nao gerou o arquivo agora.', 'warning');
        return of(null);
      })
    ).subscribe(blob => {
      if (!blob) return;
      this.triggerDownload(blob, filename);
      this.addLocalToastNotification('Exportacao concluida', `Arquivo ${format} gerado pelo reporting-service.`, 'success');
    });
  }

  private currentMonth(): string {
    return new Date().toISOString().slice(0, 7);
  }

  private triggerDownload(blob: Blob, filename: string) {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // â”€â”€â”€ Chat â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  sendChatMessage(customText?: string) {
    const textToSend = customText || this.chatForm.get('message')?.value;
    if (!textToSend?.trim()) return;
    this.chatMessages.update(msgs => [...msgs, { role: 'user', text: textToSend, date: new Date() }]);
    if (!customText) this.chatForm.reset();
    this.chatMessages.update(msgs => [...msgs, {
      role: 'assistant',
      text: '### IA ainda nÃ£o integrada\n\nA camada de chat nÃ£o possui endpoint no gateway desta etapa.',
      date: new Date(),
      isReal: true
    }]);
    this.scrollToLatestChat();
  }

  selectSuggestion(sugar: string) { this.sendChatMessage(sugar); }

  private scrollToLatestChat() {
    setTimeout(() => {
      const el = document.getElementById('chat-history-scroll-box');
      if (el) el.scrollTop = el.scrollHeight;
    }, 100);
  }

  // â”€â”€â”€ Helpers de template â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  getRoleLabel(role: 'seller' | 'accountant' | 'seller_sec'): string {
    return { seller: 'Vendedor (Principal)', accountant: 'Contador (Somente Leitura)', seller_sec: 'Vendedor SecundÃ¡rio' }[role] || role;
  }

  getTenantInitials(): string {
    const source = this.tenant()?.sellerName || this.tenant()?.name || this.tenant()?.email || 'BR';
    return source.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p.charAt(0).toUpperCase()).join('') || 'BR';
  }

  getPlatformName(key: string): string {
    const normalized = key.toLowerCase();
    return {
      ml: 'Mercado Livre',
      'mercado-livre': 'Mercado Livre',
      mercado_livre: 'Mercado Livre',
      shopee: 'Shopee',
      amazon: 'Amazon',
      sandbox: 'Sandbox',
      manual: 'Manual',
    }[normalized] || key.toUpperCase();
  }

  getPlatformFilterOptions(): { key: string; name: string }[] {
    const apiPlatforms = this.availableFilters()?.platforms ?? [];
    const connectorPlatforms = this.connectors().map(connector => connector.key);
    const keys = Array.from(new Set([...apiPlatforms, ...connectorPlatforms, 'manual']))
      .map(key => mapPlatformKey(key))
      .filter(key => key !== 'sandbox')
      .filter(Boolean);
    return Array.from(new Set(keys)).map(key => ({ key, name: this.getPlatformName(key) }));
  }

  getStatusFilterOptions(): ReportEntryStatus[] {
    return this.availableFilters()?.statuses?.length
      ? this.availableFilters()!.statuses
      : ['PAID', 'PENDING_RELEASE', 'RECEIVED', 'CANCELLED', 'REFUNDED'];
  }

  getStatusFilterLabel(status: ReportEntryStatus): string {
    return {
      PAID: 'Pago',
      PENDING_RELEASE: 'A liberar',
      RECEIVED: 'Recebido',
      CANCELLED: 'Cancelado',
      REFUNDED: 'Reembolsado',
    }[status];
  }

  getMonthlyEvolutionMax(): number {
    return Math.max(1, ...this.monthlyEvolution().map(point => Number(point.gross_value || 0)));
  }

  getConnectorBadgeClass(connector: Connector): string {
    if (this.isMercadoLivreConnector(connector.key)) return 'bg-[#ffe600]/15 text-[#0a0b0d]';
    if (connector.key === 'sandbox') return 'bg-[#0052ff]/10 text-[#0052ff]';
    if (connector.key === 'shopee') return 'bg-[#ee4d2d]/10 text-[#ee4d2d]';
    return 'bg-[#eef0f3] text-[#5b616e]';
  }

  getConnectorDotClass(connector: Connector): string {
    return this.getPlatformColorClass(connector.key);
  }

  getConnectorStatusLabel(connector: Connector): string {
    return {
      active: 'Ativo',
      expired: 'Token expirado',
      disconnected: 'Desconectado',
      unavailable: 'Indisponivel',
    }[connector.status];
  }

  getConnectorStatusHint(connector: Connector): string {
    const messages: Record<string, string> = {
      sandbox_connector_active: 'Conector sandbox pronto para retornar pedidos padronizados.',
      mercado_livre_oauth_not_configured: 'OAuth do Mercado Livre ainda nao esta configurado no core-service.',
      mercado_livre_not_authenticated: 'Mercado Livre ainda nao autenticado para este tenant.',
      mercado_livre_token_expired: 'Token Mercado Livre expirado. Renove a conexao.',
      mercado_livre_connector_active: 'Mercado Livre conectado e validado pelo core-service.',
      status_unavailable: 'Nao foi possivel consultar o status agora.',
    };
    return messages[connector.statusMessage] || connector.statusMessage || 'Status retornado pelo core-service.';
  }

  getConnectorStatusClass(connector: Connector): string {
    return connector.active ? 'text-[#05b169]' : connector.status === 'expired' ? 'text-[#f4b000]' : 'text-gray-500';
  }

  getConnectorActionLabel(connector: Connector): string {
    return connector.active ? 'Sincronizar' : 'Configurar';
  }

  isMercadoLivreConnector(key: string): boolean {
    return ['ml', 'mercadolivre', 'mercado_livre', 'mercado-livre'].includes(key.toLowerCase());
  }

  getPlatformColorClass(key: string): string {
    const normalized = mapPlatformKey(key);
    if (normalized === 'mercado-livre') return 'bg-[#ffe600]';
    if (normalized === 'sandbox') return 'bg-[#0052ff]';
    if (normalized === 'shopee') return 'bg-[#ee4d2d]';
    if (normalized === 'amazon') return 'bg-[#0052ff]';
    if (normalized === 'manual') return 'bg-[#05b169]';
    return 'bg-[#7c828a]';
  }

  getPlatformSplitEntries(): PlatformSplitEntry[] {
    const split = this.summary()?.platform_split ?? {};
    const total = Object.values(split).reduce((sum, value) => sum + Number(value || 0), 0);
    const keys = Object.keys(split).filter(key => Number(split[key] || 0) > 0);
    const fallbackKeys = this.connectors().map(connector => connector.key);
    const visibleKeys = (keys.length ? keys : fallbackKeys).filter(key => key !== 'sandbox');
    const fallbackWidth = visibleKeys.length ? 100 / visibleKeys.length : 100;

    return visibleKeys.map(key => {
      const value = Number(split[key] || 0);
      const width = total > 0 ? (value / total) * 100 : fallbackWidth;
      return {
        key,
        name: this.getPlatformName(key),
        value,
        width: `${Math.max(width, 0)}%`,
        colorClass: this.getPlatformColorClass(key),
      };
    });
  }

  getUnreadNotificationsCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  // â”€â”€â”€ Adaptadores (backend DTO â†’ interface local do template) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  private adaptReportEntry(entry: ReportEntry): Order {
    return {
      order_id: entry.order_id || entry.id,
      platform: mapPlatformKey(entry.platform),
      date: entry.sale_date,
      gross_value: Number(entry.gross_value ?? 0),
      platform_fee: Number(entry.fee_value ?? 0),
      net_value: Number(entry.received_value ?? 0),
      payment_method: mapPaymentMethodDisplay(entry.payment_method),
      payment_date: '',
      release_date: entry.release_date ?? '',
      status: mapReportEntryStatus(entry.status),
      buyer_name: entry.buyer_name ?? '',
      items: [],
      invoice_number: entry.invoice_number ?? '',
    };
  }

  private adaptStandardOrder(order: StandardOrder): Order {
    return {
      order_id: order.order_id,
      platform: mapPlatformKey(order.platform),
      date: order.date,
      gross_value: Number(order.gross_value ?? 0),
      platform_fee: Number(order.platform_fee ?? 0),
      net_value: Number(order.net_value ?? 0),
      payment_method: mapPaymentMethodDisplay(order.payment_method),
      payment_date: order.payment_date ?? '',
      release_date: order.release_date ?? '',
      status: order.status,
      buyer_name: order.buyer_name ?? '',
      items: (order.items ?? []).map(item => ({
        name: item.title || item.sku,
        qty: item.quantity,
        unit_price: Number(item.unit_value ?? 0),
      })),
      invoice_number: order.invoice_number ?? '',
    };
  }

  private adaptFinancialSummary(
    fs: FinancialSummary,
    platformComparison: PlatformComparisonPoint[] = this.platformComparison()
  ): DashboardSummary {
    const orders = this.orders();
    const totalOrders = orders.length;
    const paidOrders = orders.filter(o => o.status === 'paid').length;
    const entryCount = Number(fs.entry_count ?? totalOrders);
    const paidCount = paidOrders || entryCount;
    const avgTicket = paidCount > 0 ? Number(fs.gross_value ?? 0) / paidCount : 0;
    const activeIntegrations = this.connectors().filter(c => c.active).map(c => c.key);

    const platformSplit: Record<string, number> = {};
    this.connectors().forEach(connector => { platformSplit[connector.key] = 0; });
    platformSplit['manual'] = platformSplit['manual'] ?? 0;
    const hasPlatformComparison = platformComparison.length > 0;
    platformComparison.forEach(point => {
      platformSplit[mapPlatformKey(point.platform)] = Number(point.gross_value ?? 0);
    });
    if (!hasPlatformComparison) {
      orders.forEach(o => { platformSplit[o.platform] = (platformSplit[o.platform] ?? 0) + o.gross_value; });
    }

    return {
      gross_value: Number(fs.gross_value ?? 0),
      platform_fee: Number(fs.fee_value ?? 0),
      net_value: Number(fs.received_value ?? 0),
      pending_value: Number(fs.receivable_value ?? 0),
      total_orders: entryCount,
      paid_orders_count: paidCount,
      average_ticket: avgTicket,
      platform_split: platformSplit,
      currency: 'BRL',
      active_integrations: activeIntegrations,
    };
  }

  private adaptNotification(msg: NotificationMessage): AppNotification {
    return {
      id: msg.id,
      title: msg.subject,
      message: msg.body,
      type: mapNotificationType(msg.type),
      date: msg.createdAt,
      read: msg.read || msg.archivedAt !== null,
    };
  }

  private adaptConnector(desc: ConnectorDescriptor, status?: ConnectorStatus): Connector {
    const key = desc.name.toLowerCase();
    const normalizedStatus = status?.status ?? 'unavailable';
    const active = normalizedStatus === 'active';
    const descriptions: Record<string, string> = {
      'mercado-livre': 'Integração com Mercado Livre via contrato padronizado do core-service.',
      sandbox: 'Conector sandbox ativo para validar pedidos, pagamentos, taxas e sync sem depender de marketplace externo.',
      shopee: 'Integração com Shopee para consolidação de vendas e repasses.',
      amazon: 'Integração com Amazon Seller Central para gestão de lançamentos e comissões.',
    };
    return {
      key,
      name: desc.display_name || this.getPlatformName(desc.name),
      active,
      version: '1',
      description: descriptions[key] || `Conector ${desc.display_name || desc.name} para sincronização de vendas.`,
      type: 'Marketplace',
      status: normalizedStatus,
      statusMessage: status?.message ?? 'status_unavailable',
      checkedAt: status?.checked_at ?? null,
      supportsInvoices: desc.supports_invoices,
      requiredMethods: desc.required_methods ?? [],
      optionalMethods: desc.optional_methods ?? [],
    };
  }

  private unavailableConnectorStatus(connectorName: string): ConnectorStatus {
    return {
      platform: connectorName,
      status: 'unavailable',
      message: 'status_unavailable',
      checked_at: new Date().toISOString(),
    };
  }

  private updateTenantConnectorFlags(connectors: Connector[]) {
    this.tenant.update(current => {
      if (!current) return current;
      const active = new Set(connectors.filter(conn => conn.active).map(conn => conn.key));
      return {
        ...current,
        mlConnected: active.has('mercado-livre') || active.has('ml'),
        shopeeConnected: active.has('shopee'),
        amazonConnected: active.has('amazon'),
      };
    });
  }

  private setOrders(orders: Order[]) {
    this.orders.set(orders);
    this.updateSummaryOrderMetrics(orders);
  }

  private updateSummaryOrderMetrics(orders: Order[] = this.orders()) {
    this.summary.update(current => {
      if (!current) return current;

      const totalOrders = orders.length;
      const paidOrders = orders.filter(order => order.status === 'paid').length;
      const platformSplit: Record<string, number> = {};
      this.connectors().forEach(connector => { platformSplit[connector.key] = 0; });
      platformSplit['manual'] = platformSplit['manual'] ?? 0;
      const comparison = this.platformComparison();
      if (comparison.length > 0) {
        comparison.forEach(point => {
          platformSplit[mapPlatformKey(point.platform)] = Number(point.gross_value ?? 0);
        });
      } else {
        orders.forEach(order => {
          platformSplit[order.platform] = (platformSplit[order.platform] ?? 0) + order.gross_value;
        });
      }

      return {
        ...current,
        total_orders: totalOrders,
        paid_orders_count: paidOrders,
        average_ticket: paidOrders > 0 ? current.gross_value / paidOrders : 0,
        platform_split: platformSplit,
        active_integrations: this.connectors().filter(connector => connector.active).map(connector => connector.key),
      };
    });
  }

  // â”€â”€â”€ Toast local â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  private addLocalToastNotification(title: string, message: string, type: 'success' | 'info' | 'warning' | 'danger') {
    const item: AppNotification = {
      id: 'n_local_' + Date.now(),
      title, message, type,
      date: new Date().toISOString(),
      read: false,
    };
    this.notifications.update(list => [item, ...list]);
  }

  // â”€â”€â”€ Markdown renderer (chat IA) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

  renderMarkdown(text: string): string {
    if (!text) return '';
    let html = text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    html = html.replace(/^### (.*$)/gim, '<h4 class="text-sm font-semibold text-gray-900 tracking-tight mb-2 mt-4">$1</h4>');
    html = html.replace(/^## (.*$)/gim,  '<h3 class="text-base font-semibold text-gray-900 tracking-tight mb-2 mt-4">$1</h3>');
    html = html.replace(/^# (.*$)/gim,   '<h2 class="text-lg font-bold text-gray-950 tracking-tight mb-3 mt-4">$1</h2>');
    html = html.replace(/\*\*(.*?)\*\*/g, '<b class="font-semibold text-gray-900">$1</b>');
    html = html.replace(/^\*\s(.*$)/gim, '<li class="ml-4 list-disc text-xs text-gray-600 mb-1">$1</li>');
    html = html.replace(/`(.*?)`/g, '<code class="px-1.5 py-0.5 bg-gray-100 font-mono text-xs text-brand rounded">$1</code>');

    const lines = text.split('\n');
    let inTable = false;
    let tableHtml = '<div class="overflow-x-auto my-3 border border-gray-100 rounded-lg"><table class="w-full text-left text-xs font-sans tracking-tight border-collapse"><thead>';
    const out: string[] = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.startsWith('|') && line.endsWith('|')) {
        const cells = line.split('|').map(c => c.trim()).filter((_, idx, arr) => idx > 0 && idx < arr.length - 1);
        if (!inTable) {
          inTable = true;
          tableHtml += '<tr class="bg-gray-50 border-b border-gray-100">';
          cells.forEach(h => { tableHtml += `<th class="px-3 py-2 text-[11px] font-semibold text-gray-500 uppercase tracking-wider">${h.replace(/\*\*/g, '')}</th>`; });
          tableHtml += '</tr></thead><tbody>';
          if (lines[i + 1]?.includes('---')) i++;
        } else {
          tableHtml += '<tr class="border-b border-gray-100 hover:bg-gray-50/50">';
          cells.forEach(v => {
            tableHtml += `<td class="px-3 py-2 text-gray-600 whitespace-nowrap">${v.replace(/\*\*(.*?)\*\*/g, '<strong class="font-medium text-gray-900">$1</strong>')}</td>`;
          });
          tableHtml += '</tr>';
        }
      } else {
        if (inTable) {
          inTable = false;
          tableHtml += '</tbody></table></div>';
          out.push(tableHtml);
          tableHtml = '<div class="overflow-x-auto my-3 border border-gray-100 rounded-lg"><table class="w-full text-left text-xs font-sans tracking-tight border-collapse"><thead>';
        }
        out.push(line);
      }
    }
    if (inTable) { tableHtml += '</tbody></table></div>'; out.push(tableHtml); }

    html = out.join('\n').split('\n').map(p => {
      if (!p.trim()) return '';
      if (p.startsWith('<li') || p.startsWith('<h') || p.startsWith('<div') || p.startsWith('<table') || p.startsWith('<tr') || p.startsWith('<td')) return p;
      return `<p class="mb-2 text-xs leading-relaxed text-gray-700">${p}</p>`;
    }).join('');

    return html;
  }
}
