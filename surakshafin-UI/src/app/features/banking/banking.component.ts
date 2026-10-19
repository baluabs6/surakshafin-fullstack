import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BankingApiService } from './banking.service';
import { AuthService } from '../../core/auth.service';
import { AccountType, BankAccountView, StatementView } from '../../core/models';
import { TransferComponent } from './transfer.component';
import { DepositsComponent } from './deposits.component';
import { EmiComponent } from './emi.component';
import { errorMessage, lastFour } from './util';

type Tab = 'accounts' | 'send' | 'deposits' | 'emi';

@Component({
  selector: 'app-banking',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, TransferComponent, DepositsComponent, EmiComponent],
  template: `
    <h1>Banking</h1>
    <p class="muted">Accounts, safer transfers, fixed deposits and loan planning — with SuRakshaFin's fraud checks built in.</p>

    <nav class="primary-nav" style="display:flex;gap:1rem;flex-wrap:wrap;margin:1rem 0;border-bottom:1px solid var(--rule);padding-bottom:0.5rem;">
      @for (t of tabs; track t.id) {
        <button type="button" [class.btn-primary]="tab() === t.id" [class.btn-quiet]="tab() !== t.id" (click)="tab.set(t.id)">{{ t.label }}</button>
      }
    </nav>

    @if (error()) { <div class="banner-error">{{ error() }}</div> }
    @if (notice()) { <div class="banner-success">{{ notice() }}</div> }

    @switch (tab()) {
      @case ('accounts') {
        @if (!kycDone()) {
          <form [formGroup]="kycForm" (ngSubmit)="verifyKyc()" class="ledger accent-gold stack">
            <h2>Verify your identity to open an account</h2>
            <p class="muted">Enter your PAN to complete the lightweight KYC step. Banking features unlock once it is done.</p>
            <div class="field">
              <label for="pan">PAN</label>
              <input id="pan" type="text" formControlName="panNumber" placeholder="ABCDE1234F" maxlength="10" style="text-transform:uppercase;" />
              @if (kycForm.controls.panNumber.touched && kycForm.controls.panNumber.invalid) {
                <span class="field-error">Enter a valid PAN, e.g. ABCDE1234F</span>
              }
            </div>
            <button class="btn-primary" type="submit" [disabled]="kycForm.invalid || busy()">Verify</button>
          </form>
        } @else {
          <div style="display:flex;gap:0.75rem;flex-wrap:wrap;margin-bottom:1rem;">
            <button class="btn-primary" (click)="open('SAVINGS')" [disabled]="busy() || accounts().length >= 3">Open savings account</button>
            <button class="btn-quiet" (click)="open('CURRENT')" [disabled]="busy() || accounts().length >= 3">Open current account</button>
          </div>
        }

        @if (kycDone() && accounts().length === 0) {
          <div class="empty-state">You have no accounts yet. Open one to get started.</div>
        }

        <div class="grid-2">
          @for (a of accounts(); track a.id) {
            <div class="ledger" [class.accent-success]="selected()?.id === a.id" [class.accent-danger]="a.status === 'FROZEN'">
              <div class="ledger-row" style="border:none;padding-top:0;">
                <div>
                  <strong>{{ a.type === 'SAVINGS' ? 'Savings' : 'Current' }} account</strong>
                  <div class="muted" style="font-size:0.85rem;">A/c {{ a.accountNumber }} · IFSC {{ a.ifsc }}</div>
                </div>
                @if (a.status === 'FROZEN') { <span class="badge badge-severity-HIGH">FROZEN</span> }
              </div>
              <div style="font-size:1.6rem;font-family:var(--font-display);">₹{{ a.balance | number:'1.2-2' }}</div>
              <div class="muted" style="font-size:0.85rem;">Daily transfer limit ₹{{ a.dailyTransferLimit | number:'1.0-0' }}</div>
              <button class="btn-quiet" style="margin-top:0.75rem;" (click)="select(a)">View statement &amp; add / withdraw cash</button>
            </div>
          }
        </div>

        @if (selected(); as acc) {
          <h2 style="margin-top:1.5rem;">A/c {{ lastFour(acc.accountNumber) }}: cash</h2>
          <form [formGroup]="cashForm" class="ledger stack">
            <div class="field">
              <label for="cashAmount">Amount (₹)</label>
              <input id="cashAmount" type="number" formControlName="amount" min="1" step="0.01" />
            </div>
            <div class="field">
              <label for="cashRemark">Note (optional)</label>
              <input id="cashRemark" type="text" formControlName="remark" maxlength="120" />
            </div>
            <div style="display:flex;gap:0.75rem;flex-wrap:wrap;">
              <button class="btn-primary" type="button" (click)="cash('deposit')" [disabled]="cashForm.invalid || busy()">Deposit</button>
              <button class="btn-quiet" type="button" (click)="cash('withdraw')" [disabled]="cashForm.invalid || busy()">Withdraw</button>
            </div>
            <p class="field-hint">Deposits are a demo funding source — there is no real money behind them.</p>
          </form>

          <h2 style="margin-top:1.5rem;">Statement</h2>
          @if (statement(); as s) {
            @if (s.entries.length === 0) {
              <div class="empty-state">No transactions in the last 30 days.</div>
            } @else {
              <div class="ledger">
                @for (e of s.entries; track e.id) {
                  <div class="ledger-row">
                    <div>
                      <strong>{{ e.description }}</strong>
                      <div class="muted" style="font-size:0.85rem;">
                        {{ e.createdAt | date:'dd MMM yyyy, HH:mm' }} · Ref {{ e.reference }}
                      </div>
                    </div>
                    <div style="text-align:right;">
                      <div [style.color]="e.type === 'CREDIT' ? 'var(--success)' : 'var(--danger)'">
                        {{ e.type === 'CREDIT' ? '+' : '−' }}₹{{ e.amount | number:'1.2-2' }}
                      </div>
                      <div class="muted" style="font-size:0.85rem;">Bal ₹{{ e.balanceAfter | number:'1.2-2' }}</div>
                    </div>
                  </div>
                }
              </div>
              <div style="display:flex;gap:0.75rem;align-items:center;flex-wrap:wrap;">
                <button class="btn-quiet" (click)="loadStatement(s.page - 1)" [disabled]="s.page === 0">Newer</button>
                <span class="muted">Page {{ s.page + 1 }} of {{ s.totalPages }}</span>
                <button class="btn-quiet" (click)="loadStatement(s.page + 1)" [disabled]="s.page + 1 >= s.totalPages">Older</button>
                <button class="btn-quiet" (click)="downloadCsv(acc)">Download CSV (last 30 days)</button>
              </div>
            }
          }
        }
      }
      @case ('send') {
        <app-transfer [accounts]="accounts()" (changed)="refreshAccounts()" />
      }
      @case ('deposits') {
        <app-deposits [accounts]="accounts()" (changed)="refreshAccounts()" />
      }
      @case ('emi') {
        <app-emi />
      }
    }
  `,
})
export class BankingComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(BankingApiService);
  private auth = inject(AuthService);

  readonly lastFour = lastFour;
  readonly tabs: { id: Tab; label: string }[] = [
    { id: 'accounts', label: 'Accounts' },
    { id: 'send', label: 'Send money' },
    { id: 'deposits', label: 'Fixed deposits' },
    { id: 'emi', label: 'Loan calculator' },
  ];

  tab = signal<Tab>('accounts');
  accounts = signal<BankAccountView[]>([]);
  selected = signal<BankAccountView | null>(null);
  statement = signal<StatementView | null>(null);
  busy = signal(false);
  error = signal<string | null>(null);
  notice = signal<string | null>(null);

  kycForm = this.fb.group({
    panNumber: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{5}\d{4}[A-Za-z]$/)]],
  });

  cashForm = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    remark: [''],
  });

  kycDone = () => !!this.auth.currentUser()?.kycLiteVerified;

  ngOnInit(): void {
    this.refreshAccounts();
  }

  refreshAccounts(): void {
    this.api.accounts().subscribe({
      next: (list) => {
        this.accounts.set(list);
        const current = this.selected();
        if (current) {
          const fresh = list.find((a) => a.id === current.id) ?? null;
          this.selected.set(fresh);
          if (fresh) this.loadStatement(this.statement()?.page ?? 0);
        }
      },
      error: (e) => this.error.set(errorMessage(e, 'Could not load your accounts.')),
    });
  }

  verifyKyc(): void {
    if (this.kycForm.invalid) return;
    this.begin();
    const pan = (this.kycForm.getRawValue().panNumber as string).toUpperCase();
    this.api.verifyKyc(pan).subscribe({
      next: (profile) => {
        this.auth.updateProfile(profile);
        this.done('KYC verified. You can now open an account.');
      },
      error: (e) => this.fail(e),
    });
  }

  open(type: AccountType): void {
    this.begin();
    this.api.openAccount(type).subscribe({
      next: () => {
        this.done('Account opened.');
        this.refreshAccounts();
      },
      error: (e) => this.fail(e),
    });
  }

  select(a: BankAccountView): void {
    this.selected.set(a);
    this.statement.set(null);
    this.loadStatement(0);
  }

  loadStatement(page: number): void {
    const acc = this.selected();
    if (!acc) return;
    this.api.statement(acc.id, Math.max(page, 0)).subscribe({
      next: (s) => this.statement.set(s),
      error: (e) => this.error.set(errorMessage(e, 'Could not load the statement.')),
    });
  }

  cash(kind: 'deposit' | 'withdraw'): void {
    const acc = this.selected();
    if (!acc || this.cashForm.invalid) return;
    const { amount, remark } = this.cashForm.getRawValue();
    this.begin();
    const call = kind === 'deposit'
      ? this.api.deposit(acc.id, amount as number, remark || undefined)
      : this.api.withdraw(acc.id, amount as number, remark || undefined);
    call.subscribe({
      next: () => {
        this.cashForm.reset({ amount: null, remark: '' });
        this.done(kind === 'deposit' ? 'Deposit successful.' : 'Withdrawal successful.');
        this.refreshAccounts();
      },
      error: (e) => this.fail(e),
    });
  }

  downloadCsv(acc: BankAccountView): void {
    this.api.statementCsv(acc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `statement-${lastFour(acc.accountNumber).replace('••', '')}.csv`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.error.set(errorMessage(e, 'Could not download the statement.')),
    });
  }

  private begin(): void {
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
  }

  private done(message: string): void {
    this.busy.set(false);
    this.notice.set(message);
  }

  private fail(e: unknown): void {
    this.busy.set(false);
    this.error.set(errorMessage(e));
  }
}
