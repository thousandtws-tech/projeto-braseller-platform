import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [
  // Raiz: redireciona para /login (o noAuthGuard leva para /dashboard se já autenticado)
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },

  // Área pública: página de autenticação
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login-page').then(m => m.LoginPage),
    canActivate: [noAuthGuard],
  },

  // Callback OAuth Google (sem guard para não bloquear o fluxo de retorno)
  {
    path: 'auth/callback',
    loadComponent: () =>
      import('./features/auth/pages/login-page').then(m => m.LoginPage),
  },

  // Área privada: dashboard principal (protegida pelo authGuard)
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard-shell').then(m => m.DashboardShell),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-home-page').then(m => m.DashboardHomePage),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/dashboard/pages/orders-page').then(m => m.OrdersPage),
      },
      {
        path: 'expenses',
        loadComponent: () =>
          import('./features/dashboard/pages/expenses-page').then(m => m.ExpensesPage),
      },
      {
        path: 'dre',
        loadComponent: () =>
          import('./features/dashboard/pages/dre-page').then(m => m.DrePage),
      },
      {
        path: 'connectors',
        loadComponent: () =>
          import('./features/dashboard/pages/connectors-page').then(m => m.ConnectorsPage),
      },
      {
        path: 'ai',
        loadComponent: () =>
          import('./features/dashboard/pages/ai-page').then(m => m.AiPage),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/dashboard/pages/profile-page').then(m => m.ProfilePage),
      },
    ],
  },

  // Futuras rotas internas do dashboard (ex: /dashboard/orders, /dashboard/chat)
  // podem ser adicionadas aqui como children de 'dashboard'

  // Página de acesso negado (role insuficiente)
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./pages/unauthorized-page').then(m => m.UnauthorizedPage),
  },

  // Wildcard: qualquer rota não encontrada
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found-page').then(m => m.NotFoundPage),
  },
];
