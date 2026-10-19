import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AccountType, ApiResponse, BankAccountView, BeneficiaryView, EmiResponse, FdQuote, FdView, StatementView,
  TransferCheckResponse, TransferMode, TransferView, UserProfile,
} from '../../core/models';

export interface TransferPayload {
  fromAccountId: number;
  toAccountNumber: string;
  toIfsc: string;
  amount: number;
  mode: TransferMode;
  remark?: string;
  idempotencyKey: string;
  acknowledgeRisk: boolean;
}

const BASE = '/api/v1/banking';

@Injectable({ providedIn: 'root' })
export class BankingApiService {
  constructor(private http: HttpClient) {}

  private unwrap<T>() {
    return map((r: ApiResponse<T>) => r.data);
  }

  verifyKyc(panNumber: string): Observable<UserProfile> {
    return this.http.post<ApiResponse<UserProfile>>('/api/v1/users/me/kyc-lite', { panNumber }).pipe(this.unwrap());
  }

  accounts(): Observable<BankAccountView[]> {
    return this.http.get<ApiResponse<BankAccountView[]>>(`${BASE}/accounts`).pipe(this.unwrap());
  }

  openAccount(type: AccountType): Observable<BankAccountView> {
    return this.http.post<ApiResponse<BankAccountView>>(`${BASE}/accounts`, { type }).pipe(this.unwrap());
  }

  deposit(id: number, amount: number, remark?: string): Observable<BankAccountView> {
    return this.http.post<ApiResponse<BankAccountView>>(`${BASE}/accounts/${id}/deposit`, { amount, remark }).pipe(this.unwrap());
  }

  withdraw(id: number, amount: number, remark?: string): Observable<BankAccountView> {
    return this.http.post<ApiResponse<BankAccountView>>(`${BASE}/accounts/${id}/withdraw`, { amount, remark }).pipe(this.unwrap());
  }

  statement(id: number, page: number, size = 10): Observable<StatementView> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<StatementView>>(`${BASE}/accounts/${id}/statement`, { params }).pipe(this.unwrap());
  }

  /** Fetched through HttpClient (not a plain link) so the Authorization header is attached. */
  statementCsv(id: number): Observable<Blob> {
    return this.http.get(`${BASE}/accounts/${id}/statement.csv`, { responseType: 'blob' });
  }

  beneficiaries(): Observable<BeneficiaryView[]> {
    return this.http.get<ApiResponse<BeneficiaryView[]>>(`${BASE}/beneficiaries`).pipe(this.unwrap());
  }

  addBeneficiary(req: { accountNumber: string; ifsc: string; nickname?: string; markTrusted: boolean }): Observable<BeneficiaryView> {
    return this.http.post<ApiResponse<BeneficiaryView>>(`${BASE}/beneficiaries`, req).pipe(this.unwrap());
  }

  removeBeneficiary(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/beneficiaries/${id}`).pipe(map(() => undefined));
  }

  checkTransfer(req: { fromAccountId: number; toAccountNumber: string; toIfsc: string; amount: number }): Observable<TransferCheckResponse> {
    return this.http.post<ApiResponse<TransferCheckResponse>>(`${BASE}/transfers/check`, req).pipe(this.unwrap());
  }

  transfer(req: TransferPayload): Observable<TransferView> {
    return this.http.post<ApiResponse<TransferView>>(`${BASE}/transfers`, req).pipe(this.unwrap());
  }

  transfers(): Observable<TransferView[]> {
    return this.http.get<ApiResponse<TransferView[]>>(`${BASE}/transfers`).pipe(this.unwrap());
  }

  fdQuote(principal: number, tenureMonths: number): Observable<FdQuote> {
    const params = new HttpParams().set('principal', principal).set('tenureMonths', tenureMonths);
    return this.http.get<ApiResponse<FdQuote>>(`${BASE}/fixed-deposits/quote`, { params }).pipe(this.unwrap());
  }

  bookFd(accountId: number, principal: number, tenureMonths: number): Observable<FdView> {
    return this.http.post<ApiResponse<FdView>>(`${BASE}/fixed-deposits`, { accountId, principal, tenureMonths }).pipe(this.unwrap());
  }

  fixedDeposits(): Observable<FdView[]> {
    return this.http.get<ApiResponse<FdView[]>>(`${BASE}/fixed-deposits`).pipe(this.unwrap());
  }

  closeFd(id: number): Observable<FdView> {
    return this.http.post<ApiResponse<FdView>>(`${BASE}/fixed-deposits/${id}/close`, {}).pipe(this.unwrap());
  }

  emi(principal: number, annualRatePercent: number, tenureMonths: number): Observable<EmiResponse> {
    const params = new HttpParams().set('principal', principal).set('annualRatePercent', annualRatePercent).set('tenureMonths', tenureMonths);
    return this.http.get<ApiResponse<EmiResponse>>(`${BASE}/loans/emi`, { params }).pipe(this.unwrap());
  }
}
