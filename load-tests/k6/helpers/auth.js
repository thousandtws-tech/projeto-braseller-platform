import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, AUTH_USER } from '../config.js';

const HEADERS = { 'Content-Type': 'application/json' };

/**
 * Autentica com e-mail/senha e retorna { accessToken, refreshToken }.
 * Retorna null se o login falhar (para não abortar o teste inteiro).
 */
export function login(email = AUTH_USER.email, password = AUTH_USER.password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: HEADERS, tags: { name: 'auth_login' } },
  );

  const ok = check(res, {
    'login: status 200': (r) => r.status === 200,
    'login: tem accessToken': (r) => r.json('accessToken') !== undefined,
  });

  if (!ok) return null;

  return {
    accessToken: res.json('accessToken'),
    refreshToken: res.json('refreshToken'),
  };
}

/**
 * Renova o access token usando o refreshToken.
 */
export function refresh(refreshToken) {
  const res = http.post(
    `${BASE_URL}/api/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: HEADERS, tags: { name: 'auth_refresh' } },
  );

  check(res, {
    'refresh: status 200': (r) => r.status === 200,
    'refresh: tem novo accessToken': (r) => r.json('accessToken') !== undefined,
  });

  return res.json('accessToken');
}

/**
 * Revoga o refresh token (logout).
 */
export function logout(refreshToken) {
  const res = http.post(
    `${BASE_URL}/api/auth/logout`,
    JSON.stringify({ refreshToken }),
    { headers: HEADERS, tags: { name: 'auth_logout' } },
  );

  check(res, { 'logout: status 200': (r) => r.status === 200 });
}

/**
 * Retorna headers com Authorization Bearer para uso em requisições autenticadas.
 */
export function bearerHeaders(accessToken) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
  };
}
