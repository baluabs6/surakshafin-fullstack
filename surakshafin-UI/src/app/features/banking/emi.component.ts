import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BankingApiService } from './banking.service';
import { EmiResponse } from '../../core/models';
import { errorMessage } from './util';

@Component({
  selector: 'app-emi',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    <h2>Loan EMI calculator</h2>
    <p class="muted">Reducing-balance EMI with a full repayment schedule. This is a planning tool, not a loan offer.</p>
    @if (error()) { <div class="banner-error">{{ error() }}</div> }

    <div class="grid-2">
      <form [formGroup]="form" (ngSubmit)="calculate()" class="ledger stack">
        <div class="field">
          <label for="emiPrincipal">Loan amount (₹)</label>
          <input id="emiPrincipal" type="number" formControlName="principal" min="1000" />
        </div>
        <div class="field">
          <label for="emiRate">Interest rate (% per year)</label>
          <input id="emiRate" type="number" formControlName="rate" min="0" max="60" step="0.05" />
        </div>
        <div class="field">
          <label for="emiMonths">Tenure (months)</label>
          <input id="emiMonths" type="number" formControlName="months" min="1" max="360" />
        </div>
        <button class="btn-primary" type="submit" [disabled]="form.invalid">Calculate</button>
      </form>

      @if (result(); as r) {
        <div class="ledger accent-gold">
          <div class="muted">Monthly EMI</div>
          <div style="font-size:1.8rem;font-family:var(--font-display);">₹{{ r.emi | number:'1.2-2' }}</div>
          <div class="ledger-row"><span>Total interest</span><span>₹{{ r.totalInterest | number:'1.2-2' }}</span></div>
          <div class="ledger-row"><span>Total payable</span><span>₹{{ r.totalPayable | number:'1.2-2' }}</span></div>
        </div>
      }
    </div>

    @if (result(); as r) {
      <h3 style="margin-top:1.5rem;">Repayment schedule</h3>
      <div class="ledger">
        @for (row of visibleRows(r); track row.number) {
          <div class="ledger-row">
            <div>Month {{ row.number }}</div>
            <div class="muted" style="font-size:0.85rem;">Principal ₹{{ row.principalPart | number:'1.2-2' }} · Interest ₹{{ row.interestPart | number:'1.2-2' }}</div>
            <div>Balance ₹{{ row.balance | number:'1.2-2' }}</div>
          </div>
        }
      </div>
      @if (r.schedule.length > 12) {
        <button class="btn-quiet" (click)="showAll.set(!showAll())">{{ showAll() ? 'Show first 12 months' : 'Show all ' + r.schedule.length + ' months' }}</button>
      }
    }
  `,
})
export class EmiComponent {
  private fb = inject(FormBuilder);
  private api = inject(BankingApiService);

  form = this.fb.group({
    principal: [500000, [Validators.required, Validators.min(1000)]],
    rate: [9, [Validators.required, Validators.min(0), Validators.max(60)]],
    months: [60, [Validators.required, Validators.min(1), Validators.max(360)]],
  });

  result = signal<EmiResponse | null>(null);
  error = signal<string | null>(null);
  showAll = signal(false);

  visibleRows(r: EmiResponse) {
    return this.showAll() ? r.schedule : r.schedule.slice(0, 12);
  }

  calculate(): void {
    if (this.form.invalid) return;
    const { principal, rate, months } = this.form.getRawValue();
    this.error.set(null);
    this.api.emi(principal as number, rate as number, months as number).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showAll.set(false);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
