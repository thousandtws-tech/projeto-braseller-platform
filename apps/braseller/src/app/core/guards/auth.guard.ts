import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Guard de Autenticação Funcional (CanActivateFn)
 * Impede que usuários não autenticados acessem rotas privadas.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = typeof window !== 'undefined' ? localStorage.getItem('braseller_access_token') : null;
  const tInfo = typeof window !== 'undefined' ? localStorage.getItem('braseller_tenant') : null;

  if (token && tInfo) {
    return true; // Autenticado e com contexto!
  }

  // Redireciona para o login e armazena a url de redirecionamento posterior
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/**
 * Guard de Funções / Autorizações de Acesso (Role-Based Access Control)
 * Restringe acesso de rotas dependendo da role do usuário logado (ex: só administradores, vendedores, contadores)
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const router = inject(Router);
    if (typeof window === 'undefined') return false;

    try {
      const saved = localStorage.getItem('braseller_tenant');
      if (saved) {
        const tenant = JSON.parse(saved);
        const userRole = tenant.role;

        if (allowedRoles.includes(userRole)) {
          return true; // Role permitida!
        }
      }
    } catch (e) {
      console.error('Falha de decodificação na segurança de rotas:', e);
    }

    // Redireciona para feedback de acesso não autorizado
    router.navigate(['/unauthorized']);
    return false;
  };
};

/**
 * Tenant Guard
 * Garante que a aplicação possui um contexto de tenant ativo no LocalStorage corporativo
 */
export const tenantGuard: CanActivateFn = () => {
  const router = inject(Router);
  if (typeof window === 'undefined') return true;

  try {
    const saved = localStorage.getItem('braseller_tenant');
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed && parsed.id) {
        return true; // Contexto de tenant ativo e íntegro
      }
    }
  } catch (e) {
    console.error('Falha de verificação de Tenant Context:', e);
  }

  router.navigate(['/login']);
  return false;
};
