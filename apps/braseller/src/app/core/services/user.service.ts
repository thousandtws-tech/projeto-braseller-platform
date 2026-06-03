import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AccountantAccessView,
  GrantAccountantAccessRequest,
  UserView,
} from '../models/user.model';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly api = inject(ApiService);

  listTenantMembers(tenantId: string): Observable<UserView[]> {
    return this.api.get<UserView[]>(`users/tenants/${tenantId}/members`);
  }

  grantAccountantAccess(
    tenantId: string,
    request: GrantAccountantAccessRequest
  ): Observable<AccountantAccessView> {
    return this.api.post<AccountantAccessView>(`users/tenants/${tenantId}/accountants`, request);
  }
}
