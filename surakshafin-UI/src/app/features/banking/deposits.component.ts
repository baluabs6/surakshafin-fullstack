import { Component, EventEmitter, Input, OnChanges, OnInit, Output, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, filter, of, switchMap, catchError } from 'rxjs';
import { BankingApiService } from './banking.service';
import { BankAccountView, FdQuote, FdView } from '../../core/models';
import { errorMessage, lastFour } from './util';

@Component({
  selector: 'app-deposits',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  template: `
    <h2>Fixed deposits</h2>
    <p class="muted">Lock money for 3 months to 10 years at a fixed rate, compounded quarterly. Breaking early earns a lower rate (1 point less), and nothing if closed within 7 days.</p>
    @if (error()) { <div class="banner-error">{{ error() }}</div> }
    @if (notice()) { <div class="banner-success">{{ notice() }}</div> }

    <div class="grid-2">
      <form [formGroup]="form" (ngSubmit)="book()" class="ledger accent-gold stack">
        <div class="field">
          <label for="fdAccount">Pay from</label>
          <select id="fdAccount" formControlName="accountId">
            @for (a of accounts; track a.id) {
              <option [ngValue]="a.id">{{ a.type === 'SAVINGS' ? 'Savings' : 'Current' }} {{ lastFour(a.accountNumber) }} — ₹{{ a.balance | number:'1.2-2' }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="fdPrincipal">Amount (₹, minimum 1,000)</label>
          <input id="fdPrincipal" type="number" formControlName="principal" min="1000" step="0.01" />
        </div>
        <div class="field">
          <label for="fdMonths">Tenure (months, 3 to 120)</label>
          <input id="fdMonths" type="number" formControlName="months" min="3" max="120" />
        </div>
        <button class="btn-primary" type="submit" [disabled]="form.invalid || busy() || accounts.length === 0">Book fixed deposit</button>
        @if (accounts.length === 0) { <p class="field-hint">Open an account first.</p> }
      </form>

      @if (quote(); as q) {
        <div class="ledger">
          <div class="muted">You'll get on {{ q.maturityDate | date:'dd MMM yyyy' }}</div>
          <div style="font-size:1.8rem;font-family:var(--font-display);">₹{{ q.maturityAmount | number:'1.2-2' }}</div>
          <div class="ledger-row"><span>Interest rate</span><span>{{ q.ratePercent | number:'1.2-2' }}% p.a.</span></div>
          <div class="ledger-row"><span>Interest earned</span><span>₹{{ q.interestEarned | number:'1.2-2' }}</span></div>
        </div>
      }
    </div>

    <h3 style="margin-top:1.5rem;">Your deposits</h3>
    @if (fds().length === 0) {
      <div class="empty-state">No fixed deposits yet.</div>
    } @else {
      <div class="ledger">
        @for (f of fds(); track f.id) {
          <div class="ledger-row">
            <div>
              <strong>₹{{ f.principal | number:'1.2-2' }}</strong> at {{ f.ratePercent | number:'1.2-2' }}% for {{ f.tenureMonths }} months
              <div class="muted" style="font-size:0.85rem;">
                {{ f.startDate | date:'dd MMM yyyy' }} → {{ f.maturityDate | date:'dd MMM yyyy' }} · {{ f.status.replace('_', ' ') }}
                @if (f.payoutAmount !== null) { · paid out ₹{{ f.payoutAmount | number:'1.2-2' }} }
              </div>
            </div>
            @if (f.status === 'ACTIVE') {
              <button class="btn-danger" (click)="close(f)" [disabled]="busy()">Close</button>
            } @else {
              <span class="muted">₹{{ f.maturityAmount | number:'1.2-2' }} at maturity</span>
            }
          </div>
        }
      </div>
    }
  `,
})
export class DepositsComponent implements OnInit, OnChanges {
  private fb = inject(FormBuilder);
  private api = inject(BankingApiService);

  @Input() accounts: BankAccountView[] = [];
  @Output() changed = new EventEmitter<void>();

  readonly lastFour = lastFour;
  fds = signal<FdView[]>([]);
  quote = signal<FdQuote | null>(null);
  busy = signal(false);
  error = signal<string | null>(null);
  notice = signal<string | null>(null);

  form = this.fb.group({
    accountId: [null as number | null, Validators.required],
    principal: [10000 as number | null, [Validators.required, Validators.min(1000)]],
    months: [12 as number | null, [Validators.required, Validators.min(3), Validators.max(120)]],
  });

  ngOnChanges(): void {
    const current = this.form.controls.accountId.value;
    if (this.accounts.length > 0 && !this.accounts.some((a) => a.id === current)) {
      this.form.patchValue({ accountId: this.accounts[0].id });
    }
  }

  ngOnInit(): void {
    this.loadFds();

    this.form.valueChanges
      .pipe(
        debounceTime(300),
        filter(() => this.form.controls.principal.valid && this.form.controls.months.valid),
        switchMap(() => {
          const { principal, months } = this.form.getRawValue();
          return this.api.fdQuote(principal as number, months as number).pipe(catchError(() => of(null)));
        }),
      )
      .subscribe((q) => this.quote.set(q));

    // Show a quote for the defaults straight away.
    this.api.fdQuote(10000, 12).pipe(catchError(() => of(null))).subscribe((q) => this.quote.set(q));
  }

  loadFds(): void {
    this.api.fixedDeposits().subscribe({
      next: (list) => this.fds.set(list),
      error: (e) => this.error.set(errorMessage(e, 'Could not load your deposits.')),
    });
  }

  book(): void {
    if (this.form.invalid) return;
    const { accountId, principal, months } = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api.bookFd(accountId as number, principal as number, months as number).subscribe({
      next: () => {
        this.busy.set(false);
        this.notice.set('Fixed deposit booked.');
        this.loadFds();
        this.changed.emit();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  close(f: FdView): void {
    if (!confirm('Close this fixed deposit now? If it has not matured you will receive a reduced amount.')) return;
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api.closeFd(f.id).subscribe({
      next: (closed) => {
        this.busy.set(false);
        this.notice.set(`Fixed deposit closed. ₹${closed.payoutAmount} was credited to your account.`);
        this.loadFds();
        this.changed.emit();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
