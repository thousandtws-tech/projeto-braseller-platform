import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, throwError, Observable } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { environment } from '../config/environment';

// Flag para controle do fluxo de refresh do token em múltiplas requisições simultâneas
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

/**
 * Interceptor de Autenticação Funcional (Angular 21 style)
 * Responsável por:
 * 1. Injetar o Token JWT Bearer em todas as requisições, exceto nas públicas ou de auth.
 * 2. Tratar instabilidades ou expiração de token (HTTP 401) com rotação / Refresh Token automático.
 * 3. Enqueueing (Fila de requisições) enquanto o token está sendo renovado.
 */
export const enterpriseAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  
  const token = typeof window !== 'undefined' ? localStorage.getItem('braseller_access_token') : null;
  const isAuthPath = req.url.includes('/auth/login') || 
                     req.url.includes('/auth/register') || 
                     req.url.includes('/auth/refresh') ||
                     req.url.includes('/auth/oauth');

  let activeRequest = req;

  // Injeção segura do JWT Bearer token
  if (token && !isAuthPath) {
    activeRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        [environment.tenantHeaderKey]: getActiveTenantId() || ''
      }
    });
  }

  return next(activeRequest).pipe(
    catchError((error) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthPath) {
        return handle401Error(activeRequest, next, router);
      }
      return throwError(() => error);
    })
  );
};

/**
 * Trata o erro 401 Unauthorized de forma segura utilizando lock e enfileiramento
 */
function handle401Error(req: HttpRequest<unknown>, next: HttpHandlerFn, router: Router): Observable<HttpEvent<unknown>> {
  const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('braseller_refresh_token') : null;

  if (!refreshToken) {
    // Sem token de refresh disponível. Redireciona imediatamente
    clearSessionAndRedirect(router);
    return throwError(() => new Error('Sessão expirada e sem Refresh Token.'));
  }

  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    // Efetua a renovação de tokens via fetch encapsulado sob RxJS
    return fetchNewTokens(refreshToken).pipe(
      switchMap((res: { accessToken: string; refreshToken: string }) => {
        isRefreshing = false;
        
        localStorage.setItem('braseller_access_token', res.accessToken);
        localStorage.setItem('braseller_refresh_token', res.refreshToken);
        
        refreshTokenSubject.next(res.accessToken);

        // Clona e repete a requisição interceptada original com o novo token
        return next(req.clone({
          setHeaders: {
            Authorization: `Bearer ${res.accessToken}`
          }
        }));
      }),
      catchError((refreshErr) => {
        isRefreshing = false;
        clearSessionAndRedirect(router);
        return throwError(() => refreshErr);
      })
    );
  } else {
    // Enquanto o refresh está em andamento, aguarda o preenchimento do subject e repete
    return refreshTokenSubject.pipe(
      filter(t => t !== null),
      take(1),
      switchMap((jwt) => {
        return next(req.clone({
          setHeaders: {
            Authorization: `Bearer ${jwt}`
          }
        }));
      })
    );
  }
}

/**
 * Efetua a chamada HTTP nativa isolada de refresh para evitar loops
 */
function fetchNewTokens(refreshToken: string): Observable<{ accessToken: string; refreshToken: string }> {
  return new Observable((subscriber) => {
    fetch(`${environment.apiUrl}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
    .then(r => {
      if (!r.ok) throw new Error('Refresh Token rejeitado ou inválido no Gateway.');
      return r.json();
    })
    .then(data => {
      subscriber.next(data);
      subscriber.complete();
    })
    .catch(err => {
      subscriber.error(err);
    });
  });
}

/**
 * Retorna o Tenant ID ativo do LocalStorage corporativo
 */
function getActiveTenantId(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    const saved = localStorage.getItem('braseller_tenant');
    if (saved) {
      const parsed = JSON.parse(saved);
      return parsed.id || null;
    }
  } catch (e) {
    console.warn('Erro ao obter metadata do Tenant:', e);
  }
  return null;
}

/**
 * Limpa estado local e redireciona para login principal
 */
function clearSessionAndRedirect(router: Router) {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('braseller_access_token');
    localStorage.removeItem('braseller_refresh_token');
    localStorage.removeItem('braseller_tenant');
  }
  router.navigate(['/login']);
}
