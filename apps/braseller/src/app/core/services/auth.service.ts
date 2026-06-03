import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  AuthLoginRequest,
  AuthRegisterRequest,
  AuthSession,
  AuthTokenSet,
  AuthenticatedUser,
  GoogleAuthorizeUrlResponse,
} from '../models/auth.model';
import { Tenant } from '../models/user.model';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly api = inject(ApiService);

  private readonly accessTokenKey = 'braseller_access_token';
  private readonly refreshTokenKey = 'braseller_refresh_token';
  private readonly sessionKey = 'braseller_auth_session';
  private readonly tenantKey = 'braseller_tenant';
  private readonly pendingGoogleTenantKey = 'braseller_pending_google_tenant';

  login(request: AuthLoginRequest): Observable<AuthSession> {
    return this.api.post<AuthTokenSet>('auth/login', request).pipe(
      map((tokens) => this.toSession(tokens)),
      tap((session) => this.persistSession(session))
    );
  }

  register(request: AuthRegisterRequest): Observable<AuthSession> {
    return this.api.post<AuthTokenSet>('auth/register', request).pipe(
      map((tokens) => this.toSession(tokens, request.tenantName)),
      tap((session) => this.persistSession(session))
    );
  }

  startGoogleLogin(tenantName?: string): Observable<void> {
    return this.api.get<GoogleAuthorizeUrlResponse>('auth/oauth/google/authorize-url').pipe(
      tap(() => this.storePendingGoogleTenant(tenantName)),
      map((response) => {
        if (!response.authorizeUrl) {
          throw new Error('OAuth Google nao esta configurado no auth-service.');
        }
        if (typeof window !== 'undefined') {
          window.location.assign(response.authorizeUrl);
        }
      })
    );
  }

  completeGoogleCallback(code: string): Observable<AuthSession> {
    const tenantName = this.consumePendingGoogleTenant();
    return this.api.post<AuthTokenSet>('auth/oauth/google/callback', { code, tenantName }).pipe(
      map((tokens) => this.toSession(tokens, tenantName)),
      tap((session) => this.persistSession(session))
    );
  }

  refresh(refreshToken: string): Observable<AuthSession> {
    return this.api.post<AuthTokenSet>('auth/refresh', { refreshToken }).pipe(
      map((tokens) => this.toSession(tokens)),
      tap((session) => this.persistSession(session))
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearSession();
      return of(void 0);
    }

    return this.api.post<{ revoked: boolean }>('auth/logout', { refreshToken }).pipe(
      catchError(() => of({ revoked: false })),
      tap(() => this.clearSession()),
      map(() => void 0)
    );
  }

  restoreSession(): AuthSession | null {
    if (typeof window === 'undefined') {
      return null;
    }

    try {
      const raw = localStorage.getItem(this.sessionKey);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as AuthSession;
      if (!parsed.accessToken || !parsed.refreshToken || !parsed.tenant?.id || !parsed.user?.id) {
        this.clearSession();
        return null;
      }
      return parsed;
    } catch {
      this.clearSession();
      return null;
    }
  }

  getAccessToken(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem(this.accessTokenKey);
  }

  getRefreshToken(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem(this.refreshTokenKey);
  }

  clearSession(): void {
    if (typeof window === 'undefined') {
      return;
    }
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.sessionKey);
    localStorage.removeItem(this.tenantKey);
    localStorage.removeItem(this.pendingGoogleTenantKey);
  }

  private persistSession(session: AuthSession): void {
    if (typeof window === 'undefined') {
      return;
    }
    localStorage.setItem(this.accessTokenKey, session.accessToken);
    localStorage.setItem(this.refreshTokenKey, session.refreshToken);
    localStorage.setItem(this.sessionKey, JSON.stringify(session));
    localStorage.setItem(this.tenantKey, JSON.stringify(session.tenant));
  }

  private toSession(tokens: AuthTokenSet, tenantName?: string): AuthSession {
    if (!tokens.accessToken || !tokens.refreshToken) {
      throw new Error('Resposta de autenticacao sem tokens.');
    }

    const profile = tokens.profile ?? {};
    const roles = tokens.roles?.length ? tokens.roles : profile.roles ?? [];
    const tenantId = tokens.tenantId ?? profile.tenantId ?? '';
    const userId = tokens.userId ?? profile.userId ?? profile.subject ?? '';
    const email = tokens.email ?? profile.email ?? '';
    const fullName = profile.fullName ?? profile.preferredUsername ?? email;

    if (!tenantId || !userId || !email) {
      throw new Error('Resposta de autenticacao sem contexto de usuario/tenant.');
    }

    const user: AuthenticatedUser = {
      id: userId,
      tenantId,
      email,
      fullName,
      preferredUsername: profile.preferredUsername,
      firstName: profile.firstName,
      lastName: profile.lastName,
      pictureUrl: profile.pictureUrl,
      emailVerified: profile.emailVerified,
      roles,
    };

    const tenant: Tenant = {
      id: tenantId,
      name: tenantName?.trim() || profile.preferredUsername || fullName,
      sellerName: profile.preferredUsername || fullName,
      email,
      role: this.toTenantRole(roles),
      plan: 'Basico',
      trialDaysLeft: 0,
      mlConnected: false,
      shopeeConnected: false,
      amazonConnected: false,
    };

    return {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      expiresAt: tokens.expiresAt,
      user,
      tenant,
    };
  }

  private toTenantRole(roles: string[]): Tenant['role'] {
    const normalizedRoles = roles.map((role) => role.toLowerCase());
    if (normalizedRoles.some((role) => role.includes('contador') || role.includes('accountant'))) {
      return 'accountant';
    }
    if (normalizedRoles.some((role) => role.includes('sec'))) {
      return 'seller_sec';
    }
    return 'seller';
  }

  private storePendingGoogleTenant(tenantName?: string): void {
    if (typeof window === 'undefined') {
      return;
    }
    const trimmed = tenantName?.trim();
    if (trimmed) {
      localStorage.setItem(this.pendingGoogleTenantKey, trimmed);
    }
  }

  private consumePendingGoogleTenant(): string | undefined {
    if (typeof window === 'undefined') {
      return undefined;
    }
    const value = localStorage.getItem(this.pendingGoogleTenantKey) ?? undefined;
    localStorage.removeItem(this.pendingGoogleTenantKey);
    return value;
  }
}
