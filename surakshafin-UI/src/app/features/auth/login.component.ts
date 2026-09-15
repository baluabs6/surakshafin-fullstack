import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="ledger" style="max-width:420px;margin:2rem auto;">
      <h1>Log in</h1>
      <p class="muted">Access your fraud reports, grievances and spend tracker.</p>

      @if (error()) {
        <div class="banner-error">{{ error() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
        <div class="field">
          <label for="phone">Mobile number</label>
          <input id="phone" type="tel" formControlName="phoneNumber" placeholder="10-digit mobile number" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" />
        </div>
        <button class="btn-primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Logging in…' : 'Log in' }}
        </button>
      </form>

      <p class="muted" style="margin-top:1rem;">
        New here? <a routerLink="/register">Create an account</a>
      </p>
    </div>
  `,
})
export class LoginComponent {
  form = this.fb.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
    password: ['', Validators.required],
  });

  error = signal<string | null>(null);
  loading = signal(false);

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue() as { phoneNumber: string; password: string }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/fraud']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Could not log in. Check your number and password.');
      },
    });
  }
}
