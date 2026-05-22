import {computed, inject, Injectable, signal} from '@angular/core';
import {FormBuilder, Validators} from '@angular/forms';
import {
  AppNotification,
  AppSection,
  ChatMessage,
  Connector,
  DashboardSummary,
  ManualOrderPayload,
  Order,
  Tenant,
  UserRole,
} from '../models/braseller.models';
import {BrasellerApiService} from '../services/braseller-api.service';

@Injectable({providedIn: 'root'})
export class BrasellerFacade {
  private readonly api = inject(BrasellerApiService);
  private readonly fb = inject(FormBuilder);

  readonly tenant = signal<Tenant | null>(null);
  readonly connectors = signal<Connector[]>([]);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly notifications = signal<AppNotification[]>([]);
  readonly selectedOrder = signal<Order | null>(null);
  readonly activeSection = signal<AppSection>('dashboard');
  readonly authMode = signal<'login' | 'register'>('login');
  readonly authError = signal('');
  readonly authLoading = signal(false);
  readonly loadingSync = signal(false);
  readonly manualDialogVisible = signal(false);
  readonly orderDialogVisible = signal(false);
  readonly upgradeDialogVisible = signal(false);

  readonly unreadNotifications = computed(() => this.notifications().filter((item) => !item.read).length);
  readonly isReadOnly = computed(() => this.tenant()?.role === 'accountant');

  readonly suggestions = [
    'Qual plataforma possui as maiores taxas e qual sua receita liquida?',
    'Fazer fechamento para meu contador',
    'Previsao de faturamento para o proximo mes',
  ];

  readonly chatMessages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: 'Central de Apoio BraSeller Inteligencia. Estou conectado aos conectores e posso analisar taxas, previsoes de faturamento e fechamentos contabeis.',
      date: new Date(),
    },
  ]);
  readonly chatLoading = signal(false);

  readonly filterForm = this.fb.group({
    platform: ['all'],
    status: ['all'],
    search: [''],
  });

  readonly chatForm = this.fb.group({
    message: ['', Validators.required],
  });

  readonly manualForm = this.fb.group({
    buyer_name: ['', [Validators.required, Validators.minLength(2)]],
    item_name: ['', Validators.required],
    qty: [1, [Validators.required, Validators.min(1)]],
    unit_price: [0, [Validators.required, Validators.min(0.01)]],
    platform_fee: [0, [Validators.required, Validators.min(0)]],
    payment_method: ['PIX', Validators.required],
    status: ['paid', Validators.required],
    invoice_number: [''],
  });

  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    sellerName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  init() {
    this.loadTenant();
    this.filterForm.valueChanges.subscribe(() => this.loadOrders());
  }

  loadAll() {
    this.loadTenant();
    this.loadConnectors();
    this.loadSummary();
    this.loadOrders();
    this.loadNotifications();
  }

  loadTenant() {
    this.api.me().subscribe({
      next: (tenant) => {
        this.tenant.set(tenant);
        this.loadConnectors();
        this.loadSummary();
        this.loadOrders();
        this.loadNotifications();
      },
      error: () => this.tenant.set(null),
    });
  }

  loadConnectors() {
    this.api.connectors().subscribe({next: (items) => this.connectors.set(items)});
  }

  loadSummary() {
    this.api.summary().subscribe({next: (summary) => this.summary.set(summary)});
  }

  loadOrders() {
    const filters = this.filterForm.getRawValue();
    const params: Record<string, string> = {};

    if (filters.platform && filters.platform !== 'all') params['platform'] = filters.platform;
    if (filters.status && filters.status !== 'all') params['status'] = filters.status;
    if (filters.search?.trim()) params['search'] = filters.search.trim();

    this.api.orders(params).subscribe({next: (orders) => this.orders.set(orders)});
  }

  loadNotifications() {
    this.api.notifications().subscribe({next: (items) => this.notifications.set(items)});
  }

  submitLogin() {
    if (this.loginForm.invalid) return;

    this.authLoading.set(true);
    this.authError.set('');

    this.api.login(this.loginForm.getRawValue() as {email: string; password: string}).subscribe({
      next: (response) => {
        this.authLoading.set(false);
        if (response.success) {
          this.tenant.set(response.user);
          this.loadAll();
          this.addLocalNotification('Sessao iniciada', `Bem-vindo, ${response.user.sellerName}.`, 'success');
        }
      },
      error: (error) => {
        const message = error.error?.error || 'Credenciais incorretas ou servico indisponivel.';
        this.authLoading.set(false);
        this.authError.set(message);
      },
    });
  }

  submitRegister() {
    if (this.registerForm.invalid) return;

    this.authLoading.set(true);
    this.authError.set('');

    this.api.register(this.registerForm.getRawValue() as {name: string; sellerName: string; email: string; password: string}).subscribe({
      next: (response) => {
        this.authLoading.set(false);
        if (response.success) {
          this.tenant.set(response.user);
          this.loadAll();
          this.addLocalNotification('Cadastro concluido', `Conta criada para ${response.user.sellerName}.`, 'success');
        }
      },
      error: (error) => {
        const message = error.error?.error || 'Nao foi possivel registrar a empresa.';
        this.authLoading.set(false);
        this.authError.set(message);
      },
    });
  }

  demoLogin() {
    this.loginForm.patchValue({email: 'thousandtws@gmail.com', password: 'password123'});
    this.submitLogin();
  }

  logout() {
    this.api.logout().subscribe({
      next: () => this.tenant.set(null),
      error: () => this.tenant.set(null),
    });
  }

  switchRole(role: UserRole) {
    this.api.switchRole(role).subscribe({
      next: (response) => {
        if (response.success) {
          this.tenant.set(response.user);
          this.loadSummary();
          this.loadOrders();
          this.addLocalNotification('Perfil alterado', `Acesso reconfigurado para ${this.getRoleLabel(role)}.`, 'info');
        }
      },
    });
  }

  toggleConnector(key: string, currentStatus: boolean) {
    const nextStatus = !currentStatus;

    this.api.toggleConnector(key, nextStatus).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadAll();
          this.addLocalNotification(
            `Conector ${key.toUpperCase()} atualizado`,
            `Modulo ${nextStatus ? 'ativado' : 'desativado'} com sucesso.`,
            nextStatus ? 'success' : 'warning',
          );
        }
      },
    });
  }

  syncAll() {
    if (this.loadingSync() || this.isReadOnly()) return;

    this.loadingSync.set(true);
    this.api.syncConnectors().subscribe({
      next: (response) => {
        this.loadingSync.set(false);
        if (response.success) {
          this.loadAll();
          this.addLocalNotification('Sincronia concluida', `+${response.addedCount} lancamentos importados.`, 'success');
        } else {
          this.addLocalNotification('Falha de sincronia', response.message || 'Nao foi possivel sincronizar.', 'warning');
        }
      },
      error: () => {
        this.loadingSync.set(false);
        this.addLocalNotification('Falha de sincronia', 'Conectores responderam com timeout.', 'danger');
      },
    });
  }

  openManualOrder() {
    this.manualForm.reset({
      buyer_name: '',
      item_name: '',
      qty: 1,
      unit_price: 0,
      platform_fee: 0,
      payment_method: 'PIX',
      status: 'paid',
      invoice_number: '',
    });
    this.manualDialogVisible.set(true);
  }

  submitManualOrder() {
    if (this.manualForm.invalid) return;

    this.api.createOrder(this.manualForm.getRawValue() as ManualOrderPayload).subscribe({
      next: (response) => {
        if (response.success) {
          this.manualDialogVisible.set(false);
          this.loadAll();
          this.addLocalNotification('Lancamento criado', 'Pedido manual registrado com sucesso.', 'success');
        }
      },
      error: () => this.addLocalNotification('Falha de escrita', 'Nao foi possivel registrar o lancamento manual.', 'danger'),
    });
  }

  openOrder(order: Order) {
    this.selectedOrder.set(order);
    this.orderDialogVisible.set(true);
  }

  clearFilters() {
    this.filterForm.patchValue({platform: 'all', status: 'all', search: ''});
  }

  markAsRead(id: string) {
    this.api.markNotificationAsRead(id).subscribe({
      next: (response) => {
        if (response.success) this.notifications.set(response.notifications);
      },
    });
  }

  clearNotifications() {
    this.api.clearNotifications().subscribe({
      next: (response) => {
        if (response.success) this.notifications.set([]);
      },
    });
  }

  confirmUpgrade(plan: string) {
    this.api.upgrade(plan).subscribe({
      next: (response) => {
        if (response.success) {
          this.tenant.set(response.user);
          this.upgradeDialogVisible.set(false);
          this.addLocalNotification('Assinatura atualizada', `Plano BraSeller ${plan} ativado.`, 'success');
        }
      },
    });
  }

  sendChatMessage(customText?: string) {
    const message = customText || this.chatForm.controls.message.value;
    if (!message?.trim()) return;

    this.chatMessages.update((items) => [...items, {role: 'user', text: message, date: new Date()}]);
    if (!customText) this.chatForm.reset();

    this.chatLoading.set(true);
    this.api.askAssistant(message).subscribe({
      next: (response) => {
        this.chatLoading.set(false);
        this.chatMessages.update((items) => [
          ...items,
          {role: 'assistant', text: response.response, date: new Date(), isReal: response.isReal},
        ]);
      },
      error: () => {
        this.chatLoading.set(false);
        this.chatMessages.update((items) => [
          ...items,
          {role: 'assistant', text: 'Nao foi possivel conectar ao servidor de inteligencia analitica.', date: new Date()},
        ]);
      },
    });
  }

  exportCSV() {
    window.location.href = '/api/export/csv';
  }

  exportExcel() {
    window.location.href = '/api/export/excel';
  }

  exportPDFContador() {
    window.open('/api/export/pdf', '_blank');
  }

  getRoleLabel(role: UserRole): string {
    return {
      seller: 'Vendedor principal',
      seller_sec: 'Vendedor secundario',
      accountant: 'Contador somente leitura',
    }[role];
  }

  getPlatformName(key: string): string {
    return {
      ml: 'Mercado Livre',
      shopee: 'Shopee',
      amazon: 'Amazon',
      manual: 'Manual',
    }[key] || key.toUpperCase();
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    if (status === 'paid') return 'success';
    if (status === 'pending') return 'warn';
    if (status === 'cancelled') return 'danger';
    return 'secondary';
  }

  private addLocalNotification(title: string, message: string, type: AppNotification['type']) {
    this.notifications.update((items) => [
      {id: `local_${Date.now()}`, title, message, type, date: new Date().toISOString(), read: false},
      ...items,
    ]);
  }
}
