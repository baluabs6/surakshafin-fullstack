import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FraudApiService } from './fraud.service';
import { FraudReportView, ScamPattern } from '../../core/models';

const CATEGORIES = [
  { value: 'UPI_QR', label: 'Fake QR / UPI request' },
  { value: 'FAKE_CUSTOMER_CARE', label: 'Fake customer care call' },
  { value: 'LOAN_APP', label: 'Predatory loan app' },
  { value: 'JOB_SCAM', label: 'Work-from-home / job scam' },
  { value: 'KYC_PHISHING', label: 'KYC phishing link' },
  { value: 'OTHER', label: 'Something else' },
];

@Component({
  selector: 'app-fraud',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h1>Fraud Protection</h1>
    <p class="muted">Know the current scam patterns, and report one in a few taps.</p>

    <div class="grid-2">
      <section>
        <h2>Known scam patterns</h2>
        @if (patterns().length === 0) {
          <p class="muted">Loading…</p>
        }
        @for (p of patterns(); track p.id) {
          <div class="ledger accent-danger">
            <span class="badge" [class]="'badge-severity-' + p.severity">{{ p.severity }}</span>
            <h3 style="margin-top:0.5rem;">{{ p.title }}</h3>
            <p>{{ p.description }}</p>
          </div>
        }
      </section>

      <section>
        <h2>Report a scam or fraud</h2>
        @if (submitted()) {
          <div class="banner-success">
            Report submitted. Status: {{ submitted()!.status }}. Serious cases are forwarded to NPCI / the Cyber Crime Portal.
          </div>
        }
        @if (error()) {
          <div class="banner-error">{{ error() }}</div>
        }
        <form [formGroup]="form" (ngSubmit)="submit()" class="ledger accent-gold stack">
          <div class="field">
            <label for="category">What happened</label>
            <select id="category" formControlName="category">
              @for (c of categories; track c.value) {
                <option [value]="c.value">{{ c.label }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="details">Details</label>
            <textarea id="details" rows="4" formControlName="details" placeholder="What happened, when, and how much (if any) was involved"></textarea>
          </div>
          <div class="field">
            <label for="upi">Suspect UPI ID (optional)</label>
            <input id="upi" type="text" formControlName="suspectUpiId" placeholder="name@bank" />
          </div>
          <div class="field">
            <label for="phone">Suspect phone number (optional)</label>
            <input id="phone" type="tel" formControlName="suspectPhoneNumber" />
          </div>
          <button class="btn-danger" type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Submitting…' : 'Submit report' }}
          </button>
        </form>

        @if (myReports().length > 0) {
          <h3 style="margin-top:1.5rem;">Your past reports</h3>
          <div class="ledger">
            @for (r of myReports(); track r.id) {
              <div class="ledger-row">
                <div>
                  <strong>{{ categoryLabel(r.category) }}</strong>
                  <div class="muted" style="font-size:0.85rem;">{{ r.details }}</div>
                </div>
                <span class="badge badge-status">{{ r.status }}</span>
              </div>
            }
          </div>
        }
      </section>
    </div>
  `,
})
export class FraudComponent implements OnInit {
  categories = CATEGORIES;
  patterns = signal<ScamPattern[]>([]);
  myReports = signal<FraudReportView[]>([]);
  submitted = signal<FraudReportView | null>(null);
  error = signal<string | null>(null);
  loading = signal(false);

  form = this.fb.group({
    category: ['UPI_QR', Validators.required],
    details: ['', Validators.required],
    suspectUpiId: [''],
    suspectPhoneNumber: [''],
  });

  constructor(private fb: FormBuilder, private api: FraudApiService) {}

  ngOnInit(): void {
    this.api.patterns().subscribe((p) => this.patterns.set(p));
    this.api.myReports().subscribe((r) => this.myReports.set(r));
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    this.submitted.set(null);
    this.api.submitReport(this.form.getRawValue() as any).subscribe({
      next: (report) => {
        this.loading.set(false);
        this.submitted.set(report);
        this.myReports.update((list) => [report, ...list]);
        this.form.reset({ category: 'UPI_QR', details: '', suspectUpiId: '', suspectPhoneNumber: '' });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Could not submit the report. Please try again.');
      },
    });
  }

  categoryLabel(value: string): string {
    return this.categories.find((c) => c.value === value)?.label ?? value;
  }
}
