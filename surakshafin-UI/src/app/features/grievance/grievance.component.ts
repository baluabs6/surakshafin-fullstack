import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { GrievanceApiService } from './grievance.service';
import { GrievanceView } from '../../core/models';

const ISSUE_TYPES = [
  { value: 'UNAUTHORIZED_TXN', label: 'Unauthorised transaction' },
  { value: 'DELAYED_REFUND', label: 'Delayed refund' },
  { value: 'ACCOUNT_FREEZE', label: 'Account frozen / blocked' },
  { value: 'SERVICE_DEFICIENCY', label: 'Service deficiency' },
  { value: 'KYC_ISSUE', label: 'KYC issue' },
  { value: 'OTHER', label: 'Other' },
];

const ROUTED_TO_LABEL: Record<string, string> = {
  BANK: 'Your bank / payment provider',
  RBI_OMBUDSMAN: 'RBI Integrated Ombudsman',
  NPCI: 'NPCI Dispute Redressal',
  CYBER_CELL: 'National Cyber Crime Reporting Portal',
};

@Component({
  selector: 'app-grievance',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h1>Grievance Routing</h1>
    <p class="muted">Answer a few questions — this decides where your complaint goes and drafts it for you.</p>

    @if (result()) {
      <div class="ledger accent-success">
        <h2>Routed to: {{ routedLabel(result()!.routedTo) }}</h2>
        <p class="muted">Status: {{ result()!.status }}</p>
        <h3>Your drafted complaint</h3>
        <pre class="complaint-text">{{ result()!.generatedComplaintText }}</pre>
        <button class="btn-quiet" (click)="reset()">File another grievance</button>
      </div>
    } @else {
      @if (error()) {
        <div class="banner-error">{{ error() }}</div>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="ledger accent-gold stack" style="max-width:560px;">
        <div class="field">
          <label for="issueType">What's the issue about?</label>
          <select id="issueType" formControlName="issueType">
            @for (t of issueTypes; track t.value) {
              <option [value]="t.value">{{ t.label }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="description">Describe what happened</label>
          <textarea id="description" rows="4" formControlName="description"></textarea>
        </div>
        <div class="checkbox-field">
          <input id="fraud" type="checkbox" formControlName="suspectedFraud" />
          <label for="fraud">This involves suspected fraud or theft</label>
        </div>
        <div class="checkbox-field">
          <input id="raised" type="checkbox" formControlName="alreadyRaisedWithBank" />
          <label for="raised">I've already raised this with my bank</label>
        </div>
        @if (form.controls.alreadyRaisedWithBank.value) {
          <div class="checkbox-field">
            <input id="overdue" type="checkbox" formControlName="bankResponseOverThirtyDays" />
            <label for="overdue">It's been over 30 days with no resolution</label>
          </div>
        }
        <button class="btn-primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Filing…' : 'File grievance' }}
        </button>
      </form>
    }

    @if (history().length > 0 && !result()) {
      <h2 style="margin-top:2rem;">Your grievances</h2>
      <div class="ledger">
        @for (g of history(); track g.id) {
          <div class="ledger-row">
            <div>
              <strong>{{ g.issueType.replace('_',' ') }}</strong>
              <div class="muted" style="font-size:0.85rem;">Routed to {{ routedLabel(g.routedTo) }}</div>
            </div>
            <span class="badge badge-status">{{ g.status }}</span>
          </div>
        }
      </div>
    }
  `,
})
export class GrievanceComponent implements OnInit {
  issueTypes = ISSUE_TYPES;
  result = signal<GrievanceView | null>(null);
  history = signal<GrievanceView[]>([]);
  error = signal<string | null>(null);
  loading = signal(false);

  form = this.fb.group({
    issueType: ['UNAUTHORIZED_TXN', Validators.required],
    description: ['', Validators.required],
    alreadyRaisedWithBank: [false],
    bankResponseOverThirtyDays: [false],
    suspectedFraud: [false],
  });

  constructor(private fb: FormBuilder, private api: GrievanceApiService) {}

  ngOnInit(): void {
    this.api.mine().subscribe((g) => this.history.set(g));
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.file(this.form.getRawValue() as any).subscribe({
      next: (g) => {
        this.loading.set(false);
        this.result.set(g);
        this.history.update((list) => [g, ...list]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Could not file the grievance. Please try again.');
      },
    });
  }

  reset(): void {
    this.result.set(null);
    this.form.reset({
      issueType: 'UNAUTHORIZED_TXN', description: '', alreadyRaisedWithBank: false,
      bankResponseOverThirtyDays: false, suspectedFraud: false,
    });
  }

  routedLabel(value: string): string {
    return ROUTED_TO_LABEL[value] ?? value;
  }
}
