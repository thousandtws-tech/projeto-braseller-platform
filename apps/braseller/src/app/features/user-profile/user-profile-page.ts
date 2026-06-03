import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { AuthSession } from '../../core/models/auth.model';
import { UserView, AccountantAccessView } from '../../core/models/user.model';

@Component({
  selector: 'app-user-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-profile-page.html',
})
export class UserProfilePage implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private fb = inject(FormBuilder);

  session = signal<AuthSession | null>(null);
  members = signal<UserView[]>([]);
  isLoadingMembers = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  submitError = signal<string>('');
  submitSuccess = signal<string>('');
  showAccountantForm = signal<boolean>(false);
  lastGranted = signal<AccountantAccessView | null>(null);

  accountantForm!: FormGroup;

  ngOnInit(): void {
    this.session.set(this.authService.restoreSession());
    this.initForm();
    this.loadMembers();
  }

  private initForm(): void {
    this.accountantForm = this.fb.group({
      email:             ['', [Validators.required, Validators.email]],
      firstName:         ['', [Validators.required, Validators.minLength(2)]],
      lastName:          ['', [Validators.required, Validators.minLength(2)]],
      temporaryPassword: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  /** GET /users/tenants/{tenantId}/members */
  loadMembers(): void {
    const tenantId = this.session()?.tenant?.id;
    if (!tenantId) return;

    this.isLoadingMembers.set(true);
    this.userService.listTenantMembers(tenantId).pipe(
      catchError(() => of([] as UserView[])),
      finalize(() => this.isLoadingMembers.set(false))
    ).subscribe(list => this.members.set(Array.isArray(list) ? list : []));
  }

  /**
   * POST /users/tenants/{tenantId}/accountants
   * Envia firstName e lastName separados para que o user-service possa
   * criar o usuário corretamente no Keycloak (realm brasaller exige ambos os campos).
   * fullName é derivado da concatenação para compatibilidade com o campo da API.
   */
  grantAccountantAccess(): void {
    if (this.accountantForm.invalid) return;
    const tenantId = this.session()?.tenant?.id;
    if (!tenantId) return;

    this.isSubmitting.set(true);
    this.submitError.set('');
    this.submitSuccess.set('');

    const { email, firstName, lastName, temporaryPassword } = this.accountantForm.getRawValue() as {
      email: string;
      firstName: string;
      lastName: string;
      temporaryPassword: string;
    };

    const firstNameTrimmed = firstName.trim();
    const lastNameTrimmed  = lastName.trim();
    const fullName         = `${firstNameTrimmed} ${lastNameTrimmed}`;

    this.userService.grantAccountantAccess(tenantId, {
      email,
      firstName: firstNameTrimmed,
      lastName:  lastNameTrimmed,
      fullName,
      temporaryPassword,
    }).pipe(
      catchError(err => {
        this.submitError.set(err.message || 'Nao foi possivel conceder o acesso. Verifique os dados e tente novamente.');
        return of(null);
      }),
      finalize(() => this.isSubmitting.set(false))
    ).subscribe(result => {
      if (result) {
        this.lastGranted.set(result);
        this.submitSuccess.set(`Acesso concedido para ${fullName} (${email}) com sucesso.`);
        this.accountantForm.reset();
        this.showAccountantForm.set(false);
        this.loadMembers();
      }
    });
  }

  toggleAccountantForm(): void {
    this.showAccountantForm.update(v => !v);
    this.submitError.set('');
    this.submitSuccess.set('');
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  get canManageTeam(): boolean {
    const role = this.session()?.tenant?.role;
    return role === 'seller' || role === 'seller_sec';
  }

  getInitials(name: string | undefined | null): string {
    return (name || 'BR')
      .split(/\s+/).filter(Boolean).slice(0, 2)
      .map(p => p.charAt(0).toUpperCase()).join('') || 'BR';
  }

  getRoleLabel(role: string | undefined): string {
    switch (role) {
      case 'seller':      return 'Vendedor Principal';
      case 'seller_sec':  return 'Vendedor Secundario';
      case 'accountant':  return 'Contador';
      default:            return role ?? '-';
    }
  }

  getMemberRoleLabel(roles: string[]): string {
    if (!roles?.length) return '-';
    const r = roles.map(x => x.toUpperCase());
    if (r.some(x => x.includes('ADMIN')))                                        return 'Administrador';
    if (r.some(x => x.includes('CONTADOR') || x.includes('ACCOUNTANT')))        return 'Contador';
    if (r.some(x => x.includes('SEC')))                                          return 'Vendedor Secundario';
    if (r.some(x => x.includes('VENDEDOR') || x.includes('SELLER')))            return 'Vendedor';
    return roles[0];
  }

  getMemberRoleBadgeClass(roles: string[]): string {
    if (!roles?.length) return 'bg-gray-100 text-gray-500';
    const r = roles.map(x => x.toUpperCase());
    if (r.some(x => x.includes('ADMIN')))                                 return 'bg-[#0052ff]/10 text-[#0052ff]';
    if (r.some(x => x.includes('CONTADOR') || x.includes('ACCOUNTANT'))) return 'bg-amber-50 text-amber-700';
    return 'bg-green-50 text-[#05b169]';
  }

  isMemberActive(status: string | undefined | null): boolean {
    if (!status) return true;
    const s = status.toLowerCase().trim();
    return s === 'active' || s === 'ativo' || s === 'activated' || s === 'enabled';
  }

  getMemberStatusLabel(status: string | undefined | null): string {
    if (!status) return 'Ativo';
    const s = status.toLowerCase().trim();
    if (s === 'active' || s === 'activated' || s === 'enabled' || s === 'ativo') return 'Ativo';
    if (s === 'inactive' || s === 'disabled' || s === 'inativo')                  return 'Inativo';
    if (s === 'pending' || s === 'pendente')                                       return 'Pendente';
    if (s === 'suspended' || s === 'suspenso')                                     return 'Suspenso';
    return status;
  }
}
