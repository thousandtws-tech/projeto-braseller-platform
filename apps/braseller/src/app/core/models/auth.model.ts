import { Tenant } from './user.model';

export interface AuthLoginRequest {
  email: string;
  password: string;
}

export interface AuthRegisterRequest {
  tenantName: string;
  fullName: string;
  email: string;
  password: string;
}

export interface AuthRefreshRequest {
  refreshToken: string;
}

export interface AuthProfile {
  provider?: string;
  subject?: string;
  tenantId?: string;
  userId?: string;
  email?: string;
  fullName?: string;
  preferredUsername?: string;
  firstName?: string;
  lastName?: string;
  pictureUrl?: string;
  emailVerified?: boolean;
  roles?: string[];
}

export interface AuthTokenSet {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresAt?: string;
  tenantId?: string;
  userId?: string;
  email?: string;
  roles?: string[];
  profile?: AuthProfile;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  expiresAt?: string;
  user: AuthenticatedUser;
  tenant: Tenant;
}

export interface AuthenticatedUser {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  preferredUsername?: string;
  firstName?: string;
  lastName?: string;
  pictureUrl?: string;
  emailVerified?: boolean;
  roles: string[];
}

export interface GoogleAuthorizeUrlResponse {
  authorizeUrl: string;
}

export interface GoogleCallbackRequest {
  code: string;
  tenantName?: string;
}
