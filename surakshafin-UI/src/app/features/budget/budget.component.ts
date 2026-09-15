import { Component, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BudgetApiService } from './budget.service';
import { BudgetSummary, TransactionView } from '../../core/models';

const CATEGORIES = ['GROCERIES', 'BNPL_EMI', 'SUBSCRIPTIONS', 'DINING', 'TRAVEL', 'OTHER'];

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    <h1>Spend Tracker</h1>
    <p class="muted">Set a monthly budget and log spends — including BNPL — to get plain-language nudges.</p>

    @if (summary(); as s) {
      <div class="ledger" [class.accent-danger]="s.percentUsed >= 100" [class.accent-gold]="s.percentUsed >= 80 && s.percentUsed < 100">
        <div class="ledger-row" style="border:none;padding-top:0;">
          <div><strong>₹{{ s.spentThisSet | number:'1.0-0' }}</strong> spent<span class="muted"> of ₹{{ s.monthlyLimit | number:'1.0-0' }}</span></div>
          <div>{{ s.percentUsed }}%</div>
        </div>
        <div class="progress-track">
          <div class="progress-fill" [class.over]="s.percentUsed >= 100" [style.width.%]="minOf(s.percentUsed, 100)"></div>
        </div>
        <p style="margin-top:0.75rem;">{{ s.nudge }}</p>
      </div>
    }

    <div class="grid-2">
      <section>
        <h2>Set monthly budget</h2>
        <form [formGroup]="limitForm" (ngSubmit)="saveLimit()" class="ledger accent-gold stack">
          <div class="field">
            <label for="limit">Monthly limit (₹)</label>
            <input id="limit" type="number" formControlName="monthlyLimit" min="1" />
          </div>
          <button class="btn-primary" type="submit" [disabled]="limitForm.invalid">Save budget</button>
        </form>

        <h2>Log a transaction</h2>
        <form [formGroup]="txnForm" (ngSubmit)="addTransaction()" class="ledger stack">
          <div class="field">
            <label for="merchant">Merchant</label>
            <input id="merchant" type="text" formControlName="merchant" />
          </div>
          <div class="field">
            <label for="amount">Amount (₹)</label>
            <input id="amount" type="number" formControlName="amount" min="1" />
          </div>
          <div class="field">
            <label for="category">Category</label>
            <select id="category" formControlName="category">
              @for (c of categories; track c) {
                <option [value]="c">{{ c.replace('_',' ') }}</option>
              }
            </select>
          </div>
          <div class="checkbox-field">
            <input id="bnpl" type="checkbox" formControlName="isBnpl" />
            <label for="bnpl">This was a Buy-Now-Pay-Later purchase</label>
          </div>
          <button class="btn-primary" type="submit" [disabled]="txnForm.invalid">Log transaction</button>
        </form>
      </section>

      <section>
        <h2>Recent transactions</h2>
        @if (transactions().length === 0) {
          <div class="empty-state">No transactions logged yet.</div>
        } @else {
          <div class="ledger">
            @for (t of transactions(); track t.id) {
              <div class="ledger-row">
                <div>
                  <strong>{{ t.merchant }}</strong>
                  <div class="muted" style="font-size:0.85rem;">
                    {{ t.category.replace('_',' ') }}{{ t.isBnpl ? ' · BNPL' : '' }}
                  </div>
                </div>
                <div>₹{{ t.amount | number:'1.0-0' }}</div>
              </div>
            }
          </div>
        }
      </section>
    </div>
  `,
})
export class BudgetComponent implements OnInit {
  categories = CATEGORIES;
  summary = signal<BudgetSummary | null>(null);
  transactions = signal<TransactionView[]>([]);

  limitForm = this.fb.group({
    monthlyLimit: [15000, [Validators.required, Validators.min(1)]],
  });

  txnForm = this.fb.group({
    merchant: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    category: ['GROCERIES', Validators.required],
    isBnpl: [false],
  });

  constructor(private fb: FormBuilder, private api: BudgetApiService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.api.summary().subscribe((s) => this.summary.set(s));
    this.api.transactions().subscribe((t) => this.transactions.set(t));
  }

  saveLimit(): void {
    if (this.limitForm.invalid) return;
    this.api.setLimit(this.limitForm.getRawValue().monthlyLimit as number).subscribe(() => this.refresh());
  }

  addTransaction(): void {
    if (this.txnForm.invalid) return;
    this.api.addTransaction(this.txnForm.getRawValue() as any).subscribe(() => {
      this.txnForm.reset({ merchant: '', amount: null, category: 'GROCERIES', isBnpl: false });
      this.refresh();
    });
  }

  minOf(a: number, b: number): number {
    return Math.min(a, b);
  }
}
