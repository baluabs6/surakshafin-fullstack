import { Component, EventEmitter, Input, OnChanges, OnInit, Output, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BankingApiService } from './banking.service';
import { BankAccountView, BeneficiaryView, TransferCheckResponse, TransferView } from '../../core/models';
import { errorMessage, lastFour, newIdempotencyKey } from './util';

interface Destination {
  key: string; // "accountNumber|IFSC"
  label: string;
  accountNumber: string;
  ifsc: string;
}

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    <h2>Send money</h2>
    <p class="muted">Every transfer to someone else is screened against reported scam accounts before any money moves.</p>
    @if (error()) { <div class="banner-error">{{ error() }}</div> }
    @if (notice()) { <div class="banner-success">{{ notice() }}</div> }

    <div class="grid-2">
      <section>
        <form [formGroup]="form" (ngSubmit)="review()" class="ledger accent-gold stack">
          <div class="field">
            <label for="trFrom">From</label>
            <select id="trFrom" formControlName="fromAccountId">
              @for (a of accounts; track a.id) {
                <option [ngValue]="a.id">{{ a.type === 'SAVINGS' ? 'Savings' : 'Current' }} {{ lastFour(a.accountNumber) }} — ₹{{ a.balance | number:'1.2-2' }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="trTo">To</label>
            <select id="trTo" formControlName="to">
              <option value="">Choose a payee…</option>
              @for (d of destinations(); track d.key) {
                <option [value]="d.key">{{ d.label }}</option>
              }
            </select>
            <span class="field-hint">New payee? Add them as a beneficiary first (right).</span>
          </div>
          <div class="field">
            <label for="trAmount">Amount (₹)</label>
            <input id="trAmount" type="number" formControlName="amount" min="1" step="0.01" />
          </div>
          <div class="field">
            <label for="trMode">Mode</label>
            <select id="trMode" formControlName="mode">
              <option value="IMPS">IMPS — instant, up to ₹5,00,000</option>
              <option value="NEFT">NEFT — no upper limit</option>
              <option value="RTGS">RTGS — ₹2,00,000 and above</option>
            </select>
          </div>
          <div class="field">
            <label for="trRemark">Note (optional)</label>
            <input id="trRemark" type="text" formControlName="remark" maxlength="120" />
          </div>
          <button class="btn-primary" type="submit" [disabled]="form.invalid || busy()">Review transfer</button>
        </form>

        @if (check(); as c) {
          <div class="ledger" [class.accent-danger]="c.riskLevel === 'HIGH'" [class.accent-gold]="c.riskLevel === 'MEDIUM'" [class.accent-success]="c.riskLevel === 'LOW'" style="margin-top:1rem;">
            <h3>Review before you send</h3>
            <div class="ledger-row"><span>Payee name</span><strong>{{ c.payeeMaskedName }}</strong></div>
            <div class="ledger-row"><span>Safety check</span><span class="badge" [class]="'badge-severity-' + c.riskLevel">{{ c.riskLevel }}</span></div>

            @if (c.blocked) {
              <p style="margin-top:0.75rem;"><strong>This transfer is blocked.</strong></p>
            }
            @for (r of c.reasons; track r) { <p style="margin-top:0.5rem;">⚠ {{ r }}</p> }
            @if (c.checklistPrompts.length > 0) {
              <ul class="muted">
                @for (p of c.checklistPrompts; track p) { <li>{{ p }}</li> }
              </ul>
            }

            @if (!c.blocked) {
              @if (c.requiresAcknowledgement) {
                <div class="checkbox-field">
                  <input id="ack" type="checkbox" [checked]="acknowledged()" (change)="acknowledged.set(!acknowledged())" />
                  <label for="ack">I have checked this payee and want to continue</label>
                </div>
              }
              <button class="btn-primary" style="margin-top:0.75rem;" (click)="confirm()"
                      [disabled]="busy() || (c.requiresAcknowledgement && !acknowledged())">
                {{ busy() ? 'Sending…' : 'Confirm and send ₹' + (form.getRawValue().amount | number:'1.2-2') }}
              </button>
            }
            <button class="btn-quiet" style="margin-top:0.75rem;" (click)="cancelReview()">Cancel</button>
          </div>
        }
      </section>

      <section>
        <h3>Beneficiaries</h3>
        <form [formGroup]="benForm" (ngSubmit)="addBeneficiary()" class="ledger stack">
          <div class="field">
            <label for="benAcct">Account number</label>
            <input id="benAcct" type="text" formControlName="accountNumber" inputmode="numeric" />
          </div>
          <div class="field">
            <label for="benIfsc">IFSC</label>
            <input id="benIfsc" type="text" formControlName="ifsc" placeholder="SURK0000001" style="text-transform:uppercase;" />
          </div>
          <div class="field">
            <label for="benNick">Nickname (optional)</label>
            <input id="benNick" type="text" formControlName="nickname" maxlength="60" />
          </div>
          <div class="checkbox-field">
            <input id="benTrust" type="checkbox" formControlName="markTrusted" />
            <label for="benTrust">I know this person — add to my trusted payees</label>
          </div>
          <button class="btn-primary" type="submit" [disabled]="benForm.invalid || busy()">Add beneficiary</button>
        </form>

        @if (beneficiaries().length > 0) {
          <div class="ledger">
            @for (b of beneficiaries(); track b.id) {
              <div class="ledger-row">
                <div>
                  <strong>{{ b.nickname || b.maskedName }}</strong>
                  <div class="muted" style="font-size:0.85rem;">{{ b.maskedName }} · {{ lastFour(b.accountNumber) }} · {{ b.ifsc }}</div>
                </div>
                <button class="btn-quiet" (click)="removeBeneficiary(b)">Remove</button>
              </div>
            }
          </div>
        }
      </section>
    </div>

    <h3 style="margin-top:1.5rem;">Recent transfers</h3>
    @if (recent().length === 0) {
      <div class="empty-state">No transfers yet.</div>
    } @else {
      <div class="ledger">
        @for (t of recent(); track t.reference) {
          <div class="ledger-row">
            <div>
              <strong>{{ t.toMaskedName }}</strong> · {{ lastFour(t.toAccountNumber) }}
              <div class="muted" style="font-size:0.85rem;">{{ t.mode }} · Ref {{ t.reference }}{{ t.remark ? ' · ' + t.remark : '' }}</div>
            </div>
            <div>₹{{ t.amount | number:'1.2-2' }}</div>
          </div>
        }
      </div>
    }
  `,
})
export class TransferComponent implements OnInit, OnChanges {
  private fb = inject(FormBuilder);
  private api = inject(BankingApiService);

  @Input() accounts: BankAccountView[] = [];
  @Output() changed = new EventEmitter<void>();

  readonly lastFour = lastFour;

  beneficiaries = signal<BeneficiaryView[]>([]);
  recent = signal<TransferView[]>([]);
  check = signal<TransferCheckResponse | null>(null);
  acknowledged = signal(false);
  busy = signal(false);
  error = signal<string | null>(null);
  notice = signal<string | null>(null);

  /** One key per reviewed attempt: a double-click or network retry can never send twice. */
  private idempotencyKey = newIdempotencyKey();

  form = this.fb.group({
    fromAccountId: [null as number | null, Validators.required],
    to: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    mode: ['IMPS', Validators.required],
    remark: [''],
  });

  benForm = this.fb.group({
    accountNumber: ['', [Validators.required, Validators.pattern(/^\d{9,16}$/)]],
    ifsc: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{4}0[A-Za-z0-9]{6}$/)]],
    nickname: [''],
    markTrusted: [false],
  });

  ngOnInit(): void {
    this.loadBeneficiaries();
    this.loadRecent();
    // Any edit invalidates a review that was already shown.
    this.form.valueChanges.subscribe(() => {
      if (this.check()) this.cancelReview();
    });
  }

  ngOnChanges(): void {
    const current = this.form.controls.fromAccountId.value;
    if (this.accounts.length > 0 && !this.accounts.some((a) => a.id === current)) {
      this.form.patchValue({ fromAccountId: this.accounts[0].id }, { emitEvent: false });
    }
  }

  destinations(): Destination[] {
    const from = this.form.controls.fromAccountId.value;
    const own = this.accounts
      .filter((a) => a.id !== from)
      .map((a) => ({
        key: `${a.accountNumber}|${a.ifsc}`,
        label: `My ${a.type === 'SAVINGS' ? 'savings' : 'current'} account ${lastFour(a.accountNumber)}`,
        accountNumber: a.accountNumber,
        ifsc: a.ifsc,
      }));
    const others = this.beneficiaries().map((b) => ({
      key: `${b.accountNumber}|${b.ifsc}`,
      label: `${b.nickname || b.maskedName} (${lastFour(b.accountNumber)})`,
      accountNumber: b.accountNumber,
      ifsc: b.ifsc,
    }));
    return [...own, ...others];
  }

  loadBeneficiaries(): void {
    this.api.beneficiaries().subscribe({
      next: (list) => this.beneficiaries.set(list),
      error: (e) => this.error.set(errorMessage(e, 'Could not load beneficiaries.')),
    });
  }

  loadRecent(): void {
    this.api.transfers().subscribe({
      next: (list) => this.recent.set(list),
      error: (e) => this.error.set(errorMessage(e, 'Could not load recent transfers.')),
    });
  }

  addBeneficiary(): void {
    if (this.benForm.invalid) return;
    const v = this.benForm.getRawValue();
    this.begin();
    this.api.addBeneficiary({
      accountNumber: v.accountNumber as string,
      ifsc: (v.ifsc as string).toUpperCase(),
      nickname: v.nickname || undefined,
      markTrusted: !!v.markTrusted,
    }).subscribe({
      next: (b) => {
        this.busy.set(false);
        this.notice.set(`Added ${b.maskedName}. Check this is the right person before you send money.`);
        this.benForm.reset({ accountNumber: '', ifsc: '', nickname: '', markTrusted: false });
        this.loadBeneficiaries();
      },
      error: (e) => this.fail(e),
    });
  }

  removeBeneficiary(b: BeneficiaryView): void {
    this.api.removeBeneficiary(b.id).subscribe({
      next: () => this.loadBeneficiaries(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private selectedDestination(): Destination | undefined {
    const key = this.form.controls.to.value;
    return this.destinations().find((d) => d.key === key);
  }

  review(): void {
    const dest = this.selectedDestination();
    if (this.form.invalid || !dest) return;
    const v = this.form.getRawValue();
    this.begin();
    this.api.checkTransfer({
      fromAccountId: v.fromAccountId as number,
      toAccountNumber: dest.accountNumber,
      toIfsc: dest.ifsc,
      amount: v.amount as number,
    }).subscribe({
      next: (c) => {
        this.busy.set(false);
        this.acknowledged.set(false);
        this.idempotencyKey = newIdempotencyKey();
        this.check.set(c);
      },
      error: (e) => this.fail(e),
    });
  }

  cancelReview(): void {
    this.check.set(null);
    this.acknowledged.set(false);
  }

  confirm(): void {
    const dest = this.selectedDestination();
    const c = this.check();
    if (!dest || !c || c.blocked || this.busy()) return;
    const v = this.form.getRawValue();
    this.begin();
    this.api.transfer({
      fromAccountId: v.fromAccountId as number,
      toAccountNumber: dest.accountNumber,
      toIfsc: dest.ifsc,
      amount: v.amount as number,
      mode: v.mode as 'IMPS' | 'NEFT' | 'RTGS',
      remark: v.remark || undefined,
      idempotencyKey: this.idempotencyKey,
      acknowledgeRisk: this.acknowledged(),
    }).subscribe({
      next: (t) => {
        this.busy.set(false);
        this.notice.set(`Sent ₹${t.amount} to ${t.toMaskedName}. Reference ${t.reference}.`);
        this.cancelReview();
        this.form.reset({ fromAccountId: v.fromAccountId, to: '', amount: null, mode: 'IMPS', remark: '' });
        this.idempotencyKey = newIdempotencyKey();
        this.loadRecent();
        this.changed.emit();
      },
      error: (e) => this.fail(e),
    });
  }

  private begin(): void {
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
  }

  private fail(e: unknown): void {
    this.busy.set(false);
    this.error.set(errorMessage(e));
  }
}
