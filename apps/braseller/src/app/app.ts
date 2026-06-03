import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AuthSession } from './core/models/auth.model';
import { UserView } from './core/models/user.model';
import { AuthService } from './core/services/auth.service';
import { UserService } from './core/services/user.service';

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
}

interface OrderItem {
  name: string;
  qty: number;
  unit_price: number;
}

interface Order {
  order_id: string;
  platform: 'ml' | 'shopee' | 'amazon' | 'manual';
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

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  providers: [DecimalPipe, DatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);

  // States
  tenant = signal<Tenant | null>(null);
  members = signal<UserView[]>([]);
  connectors = signal<Connector[]>([]);
  summary = signal<DashboardSummary | null>(null);
  orders = signal<Order[]>([]);
  notifications = signal<AppNotification[]>([]);
  selectedOrder = signal<Order | null>(null);
  isOrderModalOpen = signal<boolean>(false);
  isUpgradeModalOpen = signal<boolean>(false);
  showNotificationsMenu = signal<boolean>(false);

  private ordersDb: Order[] = [];

  // Chat State
  chatMessages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: '### BraSeller IA\n\nA autenticacao e o perfil de usuario agora usam o gateway real. A assistencia analitica sera ligada ao backend em uma proxima etapa.',
      date: new Date(),
      isReal: true,
    }
  ]);
  chatLoading = signal<boolean>(false);

  // Suggested Prompts
  suggestions = [
    'Qual plataforma possui as maiores taxas e qual sua receita líquida?',
    'Fazer fechamento para meu contador',
    'Previsão de faturamento para o próximo mês'
  ];

  // Forms
  filterForm!: FormGroup;
  chatForm!: FormGroup;
  manualForm!: FormGroup;
  loginForm!: FormGroup;
  registerForm!: FormGroup;

  // Active Tab: 'dashboard' | 'connectors' | 'orders' | 'chat'
  activeTab = signal<'dashboard' | 'connectors' | 'orders' | 'chat'>('dashboard');
  
  // Loading & Modal states
  isSyncing = signal<boolean>(false);
  isManualModalOpen = signal<boolean>(false);

  // Authentication Module Signals
  authMode = signal<'login' | 'register'>('login');
  authError = signal<string>('');
  authLoading = signal<boolean>(false);

  ngOnInit() {
    this.initForms();
    
    if (typeof window !== 'undefined') {
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');
      
      if (code) {
        void this.handleOAuthCallback(code);
        return;
      }
    }

    this.loadAllData();
  }

  private initForms() {
    this.filterForm = this.fb.group({
      platform: ['all'],
      status: ['all'],
      search: [''],
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

    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      sellerName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });

    // Atualiza a lista quando os filtros mudam.
    this.filterForm.valueChanges.subscribe(() => {
      this.loadOrders();
    });
  }

  loadAllData() {
    this.loadTenant();
    this.loadConnectors();
    this.loadNotifications();
    this.recalculateSummary(this.ordersDb, this.connectors());
    this.loadOrders();
  }

  loadTenant() {
    const session = this.authService.restoreSession();
    if (session) {
      this.tenant.set(session.tenant);
      void this.loadUserMembers(session.tenant.id);
      return;
    }
    this.tenant.set(null);
    this.members.set([]);
  }

  loadConnectors() {
    this.connectors.set([]);
  }

  loadOrders() {
    const filters = this.filterForm ? this.filterForm.value : { platform: 'all', status: 'all', search: '' };
    const activeIntegrations = this.connectors().filter(c => c.active).map(c => c.key);
    
    // Filtra ordens: exibe apenas os conectores que estão atualmente ativos (mais os manuais)
    let filtered = this.ordersDb.filter(o => o.platform === 'manual' || activeIntegrations.includes(o.platform));
    
    if (filters.platform && filters.platform !== 'all') {
      filtered = filtered.filter(o => o.platform === filters.platform);
    }
    if (filters.status && filters.status !== 'all') {
      filtered = filtered.filter(o => o.status === filters.status);
    }
    if (filters.search && filters.search.trim() !== '') {
      const q = filters.search.toLowerCase();
      filtered = filtered.filter(o => 
        o.buyer_name.toLowerCase().includes(q) || 
        o.order_id.toLowerCase().includes(q) ||
        o.items.some(i => i.name.toLowerCase().includes(q))
      );
    }
    
    this.orders.set(filtered);
  }

  loadNotifications() {
    this.notifications.set([]);
  }

  private async loadUserMembers(tenantId: string): Promise<void> {
    try {
      const members = await firstValueFrom(this.userService.listTenantMembers(tenantId));
      this.members.set(Array.isArray(members) ? members : []);
    } catch (error) {
      this.members.set([]);
      console.warn('Nao foi possivel carregar membros do tenant:', error);
    }
  }

  private recalculateSummary(ordersList: Order[], connectorsList: Connector[]) {
    const activeIntegrations = connectorsList.filter(c => c.active).map(c => c.key);
    const filteredOrders = ordersList.filter(o => o.platform === 'manual' || activeIntegrations.includes(o.platform));
    
    let gross_value = 0;
    let platform_fee = 0;
    let net_value = 0;
    let pending_value = 0;
    let paid_orders_count = 0;
    
    const platform_split: Record<string, number> = { ml: 0, shopee: 0, amazon: 0, manual: 0 };
    
    filteredOrders.forEach(o => {
      if (o.status === 'paid') {
        gross_value += o.gross_value;
        platform_fee += o.platform_fee;
        net_value += o.net_value;
        paid_orders_count++;
      } else if (o.status === 'pending') {
        pending_value += o.net_value;
      }
      
      platform_split[o.platform] = (platform_split[o.platform] || 0) + o.gross_value;
    });
    
    const total_orders = filteredOrders.length;
    const average_ticket = paid_orders_count > 0 ? (gross_value / paid_orders_count) : 0;
    
    this.summary.set({
      gross_value,
      platform_fee,
      net_value,
      pending_value,
      total_orders,
      paid_orders_count,
      average_ticket,
      platform_split,
      currency: 'BRL',
      active_integrations: activeIntegrations
    });
  }

  switchRole(role: 'seller' | 'accountant' | 'seller_sec') {
    this.addLocalToastNotification(
      'Perfil gerenciado pela API',
      `O perfil ativo permanece como ${this.getRoleLabel(this.tenant()?.role || role)}. Alteracoes de papel precisam passar pelo user-service.`,
      'info'
    );
  }
  toggleConnector(key: string, currentStatus: boolean) {
    void currentStatus;
    this.addLocalToastNotification(
      `Conector ${key.toUpperCase()} indisponivel`,
      'Os conectores serao ligados ao core-service em uma proxima feature. Nenhum dado local foi alterado.',
      'warning'
    );
  }
  confirmUpgrade(plan: string) {
    this.isUpgradeModalOpen.set(false);
    this.addLocalToastNotification(
      'Billing ainda nao integrado',
      `O plano ${plan} nao foi alterado. A assinatura sera conectada ao billing-service em outra etapa.`,
      'info'
    );
  }
  // Notifications action
  markAsRead(id: string) {
    this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
  }

  clearNotifications() {
    this.notifications.set([]);
  }

  // Local ephemeral success toaster builder
  private addLocalToastNotification(title: string, message: string, type: 'success' | 'info' | 'warning' | 'danger') {
    const newItem: AppNotification = {
      id: 'n_local_' + Date.now(),
      title,
      message,
      type,
      date: new Date().toISOString(),
      read: false
    };
    this.notifications.update(list => [newItem, ...list]);
  }

  // Order popup management
  openOrderDetails(order: Order) {
    this.selectedOrder.set(order);
    this.isOrderModalOpen.set(true);
  }

  closeOrderDetails() {
    this.isOrderModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  // Sync with connectors
  syncAll() {
    this.isSyncing.set(false);
    this.addLocalToastNotification(
      'Sincronizacao indisponivel',
      'A sincronizacao de vendas sera conectada ao core-service em uma proxima feature. Nenhum dado local foi criado.',
      'info'
    );
  }

  // Manual Order insertion
  openManualOrderModal() {
    this.manualForm.reset({
      buyer_name: '',
      item_name: '',
      qty: 1,
      unit_price: 0,
      platform_fee: 0,
      payment_method: 'PIX',
      status: 'paid',
      invoice_number: ''
    });
    this.isManualModalOpen.set(true);
  }

  submitManualOrder() {
    if (this.manualForm.invalid) return;

    this.isManualModalOpen.set(false);
    this.addLocalToastNotification(
      'Lancamento manual ainda nao integrado',
      'O cadastro de lancamentos sera conectado ao core-service. Nenhum registro local foi criado.',
      'warning'
    );
  }

  async handleOAuthCallback(code: string) {
    this.authLoading.set(true);
    this.authError.set('');

    try {
      const session = await firstValueFrom(this.authService.completeGoogleCallback(code));
      this.applySession(session);
      this.clearOAuthQueryParams();
      this.addLocalToastNotification(
        'Acesso Google concluido',
        'Sessao autenticada pelo auth-service via gateway.',
        'success'
      );
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Nao foi possivel concluir o OAuth Google.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  triggerGoogleLogin() {
    this.authLoading.set(true);
    this.authError.set('');
    const tenantName = String(this.registerForm?.value?.name || '').trim() || undefined;

    firstValueFrom(this.authService.startGoogleLogin(tenantName)).catch((error) => {
      this.authLoading.set(false);
      this.authError.set(this.toErrorMessage(error, 'OAuth Google nao esta disponivel.'));
    });
  }

  async submitLogin() {
    if (this.loginForm.invalid) return;
    this.authLoading.set(true);
    this.authError.set('');

    const credentials = this.loginForm.getRawValue() as { email: string; password: string };

    try {
      const session = await firstValueFrom(this.authService.login({
        email: String(credentials.email).trim(),
        password: String(credentials.password),
      }));
      this.applySession(session);
      this.addLocalToastNotification(
        'Bem-vindo de volta',
        'Login confirmado pelo auth-service.',
        'success'
      );
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Credenciais invalidas ou servico indisponivel.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  async submitRegister() {
    if (this.registerForm.invalid) return;
    this.authLoading.set(true);
    this.authError.set('');

    const values = this.registerForm.getRawValue() as {
      name: string;
      sellerName: string;
      email: string;
      password: string;
    };

    try {
      const session = await firstValueFrom(this.authService.register({
        tenantName: String(values.name).trim(),
        fullName: String(values.sellerName).trim(),
        email: String(values.email).trim(),
        password: String(values.password),
      }));
      this.applySession(session);
      this.addLocalToastNotification(
        'Cadastro concluido',
        'Tenant e usuario criados pelo auth-service/user-service.',
        'success'
      );
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Nao foi possivel criar a conta.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  async logout() {
    await firstValueFrom(this.authService.logout());
    this.clearLocalSessionAndRedirect();
  }

  clearLocalSessionAndRedirect() {
    this.authService.clearSession();
    this.tenant.set(null);
    this.members.set([]);
    this.connectors.set([]);
    this.orders.set([]);
    this.notifications.set([]);
    this.summary.set(null);
    this.addLocalToastNotification(
      'Sessao encerrada',
      'Sua conta foi desconectada com seguranca do BraSeller.',
      'info'
    );
  }

  private applySession(session: AuthSession) {
    this.tenant.set(session.tenant);
    this.members.set([]);
    this.connectors.set([]);
    this.notifications.set([]);
    this.recalculateSummary(this.ordersDb, this.connectors());
    this.loadOrders();
    void this.loadUserMembers(session.tenant.id);
  }

  private clearOAuthQueryParams() {
    if (typeof window !== 'undefined') {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }

  private toErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error && error.message ? error.message : fallback;
  }
  // Clear query filters
  clearFilters() {
    this.filterForm.patchValue({
      platform: 'all',
      status: 'all',
      search: ''
    });
  }

  // Downloads / accountant triggers
  exportCSV() {
    let csv = 'ID da Ordem,Plataforma,Data,Valor Bruto,Taxa,Valor Liquido,Comprador,Invoice,Status\n';
    this.orders().forEach(o => {
      csv += `"${o.order_id}","${o.platform}","${o.date}",${o.gross_value},${o.platform_fee},${o.net_value},"${o.buyer_name}","${o.invoice_number}","${o.status}"\n`;
    });
    
    const blob = new Blob(["\uFEFF" + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', 'braseller_export_' + Date.now() + '.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    this.addLocalToastNotification('CSV Exportado', 'Seu arquivo de conciliacao CSV foi gerado e baixado.', 'success');
  }

  exportExcel() {
    // Generate a simple spreadsheet format compatible with Excel
    let xml = 'ID da Ordem\tPlataforma\tData\tValor Bruto\tTaxa da Plataforma\tValor Liquido\tCliente\tNF-e/NF-se\tStatus\n';
    this.orders().forEach(o => {
      xml += `${o.order_id}\t${o.platform.toUpperCase()}\t${o.date}\t${o.gross_value.toString().replace('.', ',')}\t${o.platform_fee.toString().replace('.', ',')}\t${o.net_value.toString().replace('.', ',')}\t${o.buyer_name}\t${o.invoice_number}\t${o.status.toUpperCase()}\n`;
    });

    const blob = new Blob([xml], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', 'braseller_conciliacao_' + Date.now() + '.xls');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    this.addLocalToastNotification('Excel Baixado', 'Planilha XLS estruturada gerada no navegador.', 'success');
  }

  exportPDFContador() {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      this.addLocalToastNotification('Popup Bloqueado', 'Permita popups para imprimir o relatório contábil.', 'warning');
      return;
    }

    let rowsHtml = '';
    this.orders().forEach(o => {
      rowsHtml += `
        <tr style="border-bottom: 1px solid #eee; font-size: 11px;">
          <td style="padding: 8px;">${o.order_id}</td>
          <td style="padding: 8px; text-transform: uppercase;">${o.platform}</td>
          <td style="padding: 8px;">${new Date(o.date).toLocaleDateString('pt-BR')}</td>
          <td style="padding: 8px; text-align: right;">R$ ${o.gross_value.toFixed(2)}</td>
          <td style="padding: 8px; text-align: right; color: #dc2626;">R$ ${o.platform_fee.toFixed(2)}</td>
          <td style="padding: 8px; text-align: right; font-weight: bold; color: #16a34a;">R$ ${o.net_value.toFixed(2)}</td>
          <td style="padding: 8px;">${o.buyer_name}</td>
          <td style="padding: 8px;">${o.invoice_number || '-'}</td>
          <td style="padding: 8px; text-transform: uppercase;">${o.status}</td>
        </tr>
      `;
    });

    const s = this.summary();
    const gross = s ? s.gross_value : 0;
    const fee = s ? s.platform_fee : 0;
    const net = s ? s.net_value : 0;

    printWindow.document.write(`
      <html>
        <head>
          <title>Relatório Contábil de Fechamento - BraSeller</title>
          <style>
            body { font-family: sans-serif; color: #333; margin: 40px; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th { background: #f8fafc; padding: 10px; border-bottom: 2px solid #e2e8f0; font-size: 11px; text-align: left; }
          </style>
        </head>
        <body>
          <h2 style="margin-bottom: 4px;">BraSeller Core</h2>
          <p style="font-size: 12px; color: #666; margin-top: 0;">Relatório de Conciliação de Vendas Tributárias e Lançamentos Fiscais</p>
          <hr/>
          <div style="display: flex; gap: 40px; margin-top: 20px; font-size: 12px;">
            <div><strong>Faturamento Bruto:</strong> R$ ${gross.toFixed(2)}</div>
            <div><strong>Total de Comissões Reduzido:</strong> -R$ ${fee.toFixed(2)}</div>
            <div><strong>Repasse Líquido:</strong> R$ ${net.toFixed(2)}</div>
          </div>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Plataforma</th>
                <th>Data</th>
                <th style="text-align: right;">Valor Bruto</th>
                <th style="text-align: right;">Taxa</th>
                <th style="text-align: right;">Líquido</th>
                <th>Comprador</th>
                <th>Nota Fiscal</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${rowsHtml}
            </tbody>
          </table>
          <script>window.onload = function() { window.print(); }</script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }

  // Send message to assistant backend when the feature is available
  sendChatMessage(customText?: string) {
    const textToSend = customText || this.chatForm.get('message')?.value;
    if (!textToSend || textToSend.trim() === '') return;

    this.chatMessages.update(msgs => [...msgs, {
      role: 'user',
      text: textToSend,
      date: new Date()
    }]);

    if (!customText) {
      this.chatForm.reset();
    }

    this.chatMessages.update(msgs => [...msgs, {
      role: 'assistant',
      text: '### IA ainda nao integrada\n\nA camada de chat nao possui endpoint no gateway desta etapa. Nenhuma resposta local foi gerada.',
      date: new Date(),
      isReal: true
    }]);
    this.scrollToLatestChat();
  }
  selectSuggestion(sugar: string) {
    this.sendChatMessage(sugar);
  }

  private scrollToLatestChat() {
    setTimeout(() => {
      const container = document.getElementById('chat-history-scroll-box');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  // Helper Labels & Mappings
  getRoleLabel(role: 'seller' | 'accountant' | 'seller_sec'): string {
    const labels = {
      seller: 'Vendedor (Principal)',
      accountant: 'Contador (Somente Leitura)',
      seller_sec: 'Vendedor Secundário'
    };
    return labels[role] || role;
  }

  getTenantInitials(): string {
    const source = this.tenant()?.sellerName || this.tenant()?.name || this.tenant()?.email || 'BR';
    return source
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('') || 'BR';
  }

  getPlatformName(key: string): string {
    const names = {
      ml: 'Mercado Livre',
      shopee: 'Shopee',
      amazon: 'Amazon'
    };
    return names[key as 'ml' | 'shopee' | 'amazon'] || key.toUpperCase();
  }

  getUnreadNotificationsCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  // Simple Markdown Renderer for high fidelity analytics text formatting (tables, bullet points, headers)
  renderMarkdown(text: string): string {
    if (!text) return '';
    let html = text;

    // Escape basic entities
    html = html
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h4 class="text-sm font-semibold text-gray-900 tracking-tight mb-2 mt-4">$1</h4>');
    html = html.replace(/^## (.*$)/gim, '<h3 class="text-base font-semibold text-gray-900 tracking-tight mb-2 mt-4">$1</h3>');
    html = html.replace(/^# (.*$)/gim, '<h2 class="text-lg font-bold text-gray-950 tracking-tight mb-3 mt-4">$1</h2>');

    // Bold
    html = html.replace(/\*\*(.*?)\*\*/g, '<b class="font-semibold text-gray-900">$1</b>');

    // Bullet Lists
    html = html.replace(/^\*\s(.*$)/gim, '<li class="ml-4 list-disc text-xs text-gray-600 mb-1">$1</li>');

    // Code lines / spans
    html = html.replace(/`(.*?)`/g, '<code class="px-1.5 py-0.5 bg-gray-100 font-mono text-xs text-brand rounded">$1</code>');

    // Table parsing helper (Extract markdown tables to tailored Coinbase tables)
    const lines = text.split('\n');
    let inTable = false;
    let tableHtml = '<div class="overflow-x-auto my-3 border border-gray-100 rounded-lg"><table class="w-full text-left text-xs font-sans tracking-tight border-collapse"><thead>';
    
    const renderedRowsLines: string[] = [];
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.startsWith('|') && line.endsWith('|')) {
        const cells = line.split('|').map(c => c.trim()).filter((_, idx, arr) => idx > 0 && idx < arr.length - 1);
        
        if (!inTable) {
          inTable = true;
          // Check if this is the header row
          tableHtml += '<tr class="bg-gray-50 border-b border-gray-100">';
          cells.forEach(headerCell => {
            const cleanText = headerCell.replace(/\*\*/g, '');
            tableHtml += `<th class="px-3 py-2 text-[11px] font-semibold text-gray-500 uppercase tracking-wider">${cleanText}</th>`;
          });
          tableHtml += '</tr></thead><tbody>';
          
          // Skip divider row (usually starts with | :--- |)
          if (lines[i+1] && lines[i+1].includes('---')) {
            i++; 
          }
        } else {
          // Regular data row
          tableHtml += '<tr class="border-b border-gray-100 hover:bg-gray-50/50">';
          cells.forEach((cellVal) => {
            // Apply bold formatting within cell if any
            let val = cellVal;
            val = val.replace(/\*\*(.*?)\*\*/g, '<strong class="font-medium text-gray-900">$1</strong>');
            tableHtml += `<td class="px-3 py-2 text-gray-600 whitespace-nowrap">${val}</td>`;
          });
          tableHtml += '</tr>';
        }
      } else {
        if (inTable) {
          inTable = false;
          tableHtml += '</tbody></table></div>';
          renderedRowsLines.push(tableHtml);
          tableHtml = '<div class="overflow-x-auto my-3 border border-gray-100 rounded-lg"><table class="w-full text-left text-xs font-sans tracking-tight border-collapse"><thead>';
        }
        renderedRowsLines.push(line);
      }
    }
    
    if (inTable) {
      tableHtml += '</tbody></table></div>';
      renderedRowsLines.push(tableHtml);
    }
    
    html = renderedRowsLines.join('\n');
    
    // Convert remaining single line-breaks to <br> or paragraphs
    html = html.split('\n').map(p => {
      if (p.trim() === '') return '';
      if (p.startsWith('<li') || p.startsWith('<h') || p.startsWith('<div') || p.startsWith('<table') || p.startsWith('<tr') || p.startsWith('<td')) return p;
      return `<p class="mb-2 text-xs leading-relaxed text-gray-700">${p}</p>`;
    }).join('');

    return html;
  }
}
