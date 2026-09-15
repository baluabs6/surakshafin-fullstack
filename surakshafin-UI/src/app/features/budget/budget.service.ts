import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, BudgetSummary, TransactionView } from '../../core/models';

export interface TransactionRequest {
  category: string;
  amount: number;
  merchant: string;
  isBnpl: boolean;
}

@Injectable({ providedIn: 'root' })
export class BudgetApiService {
  constructor(private http: HttpClient) {}

  addTransaction(req: TransactionRequest): Observable<TransactionView> {
    return this.http.post<ApiResponse<TransactionView>>('/api/v1/budget/transactions', req).pipe(map((r) => r.data));
  }

  transactions(): Observable<TransactionView[]> {
    return this.http.get<ApiResponse<TransactionView[]>>('/api/v1/budget/transactions').pipe(map((r) => r.data));
  }

  setLimit(monthlyLimit: number): Observable<void> {
    return this.http.put<ApiResponse<void>>('/api/v1/budget/limit', { monthlyLimit }).pipe(map(() => undefined));
  }

  summary(): Observable<BudgetSummary> {
    return this.http.get<ApiResponse<BudgetSummary>>('/api/v1/budget/summary').pipe(map((r) => r.data));
  }
}
