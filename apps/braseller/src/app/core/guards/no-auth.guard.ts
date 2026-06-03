import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Guard de Não-Autenticação (Public Route Guard)
 * Redireciona usuários já autenticados para o dashboard, impedindo que acessem /login novamente.
 */
export const noAuthGuard: CanActivateFn = () => {
  const router = inject(Router);

  if (typeof window === 'undefined') return true;

  const token = localStorage.getItem('braseller_access_token');
  const tenant = localStorage.getItem('braseller_tenant');

  if (token && tenant) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
