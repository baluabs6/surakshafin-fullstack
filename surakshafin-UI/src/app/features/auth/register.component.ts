import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="ledger" style="max-width:460px;margin:2rem auto;">
      <h1>Create your account</h1>
      <p class="muted">One account covers fraud protection, grievances, spend tracking and literacy content.</p>

      @if (error()) {
        <div class="banner-error">{{ error() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
        <div class="field">
          <label for="fullName">Full name</label>
          <input id="fullName" type="text" formControlName="fullName" />
        </div>
        <div class="field">
          <label for="phone">Mobile number</label>
          <input id="phone" type="tel" formControlName="phoneNumber" placeholder="10-digit mobile number" />
          @if (form.controls.phoneNumber.touched && form.controls.phoneNumber.invalid) {
            <div class="field-error">Enter a valid 10-digit Indian mobile number.</div>
          }
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" />
        </div>
        <div class="field">
          <label for="lang">Preferred language</label>
          <select id="lang" formControlName="preferredLanguage">
            <option value="en">English</option>
            <option value="hi">हिन्दी (Hindi)</option>
            <option value="te">తెలుగు (Telugu)</option>
            <option value="ta">தமிழ் (Tamil)</option>
            <option value="bn">বাংলা (Bengali)</option>
            <option value="mr">मराठी (Marathi)</option>
          </select>
        </div>
        <button class="btn-primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Creating account…' : 'Create account' }}
        </button>
      </form>

      <p class="muted" style="margin-top:1rem;">
        Already registered? <a routerLink="/login">Log in</a>
      </p>
    </div>
  `,
})
export class RegisterComponent {
  form = this.fb.group({
    fullName: ['', Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    preferredLanguage: ['en', Validators.required],
  });

  error = signal<string | null>(null);
  loading = signal(false);

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    this.auth
      .register(this.form.getRawValue() as { phoneNumber: string; fullName: string; password: string; preferredLanguage: string })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/fraud']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Could not create the account.');
        },
      });
  }
}
