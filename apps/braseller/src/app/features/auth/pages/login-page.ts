import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-page.html',
})
export class LoginPage implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm!: FormGroup;
  registerForm!: FormGroup;

  authMode = signal<'login' | 'register'>('login');
  authError = signal<string>('');
  authLoading = signal<boolean>(false);

  ngOnInit(): void {
    this.initForms();

    if (typeof window !== 'undefined') {
      const code = new URLSearchParams(window.location.search).get('code');
      if (code) {
        void this.handleOAuthCallback(code);
      }
    }
  }

  private initForms(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });

    this.registerForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      sellerName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  async submitLogin(): Promise<void> {
    if (this.loginForm.invalid) return;
    this.authLoading.set(true);
    this.authError.set('');

    const { email, password } = this.loginForm.getRawValue() as { email: string; password: string };

    try {
      await firstValueFrom(this.authService.login({
        email: email.trim(),
        password,
      }));
      this.router.navigate(['/dashboard']);
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Credenciais inválidas ou serviço indisponível.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  async submitRegister(): Promise<void> {
    if (this.registerForm.invalid) return;
    this.authLoading.set(true);
    this.authError.set('');

    const { name, sellerName, email, password } = this.registerForm.getRawValue() as {
      name: string; sellerName: string; email: string; password: string;
    };

    try {
      await firstValueFrom(this.authService.register({
        tenantName: name.trim(),
        fullName: sellerName.trim(),
        email: email.trim(),
        password,
      }));
      this.router.navigate(['/dashboard']);
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Não foi possível criar a conta.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  triggerGoogleLogin(): void {
    this.authLoading.set(true);
    this.authError.set('');
    const tenantName = String(this.registerForm?.value?.name || '').trim() || undefined;

    firstValueFrom(this.authService.startGoogleLogin(tenantName)).catch((error) => {
      this.authLoading.set(false);
      this.authError.set(this.toErrorMessage(error, 'OAuth Google não está disponível.'));
    });
  }

  async handleOAuthCallback(code: string): Promise<void> {
    this.authLoading.set(true);
    this.authError.set('');

    try {
      await firstValueFrom(this.authService.completeGoogleCallback(code));
      window.history.replaceState({}, document.title, window.location.pathname);
      this.router.navigate(['/dashboard']);
    } catch (error) {
      this.authError.set(this.toErrorMessage(error, 'Não foi possível concluir o OAuth Google.'));
    } finally {
      this.authLoading.set(false);
    }
  }

  private toErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error && error.message ? error.message : fallback;
  }
}
