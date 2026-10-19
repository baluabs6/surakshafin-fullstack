export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

export interface UserProfile {
  id: number;
  phoneNumber: string;
  fullName: string;
  preferredLanguage: string;
  kycLiteVerified: boolean;
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  profile: UserProfile;
}

export interface ScamPattern {
  id: number;
  title: string;
  description: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export interface FraudReportView {
  id: number;
  category: string;
  details: string;
  status: string;
  createdAt: string;
}

export interface GrievanceView {
  id: number;
  issueType: string;
  description: string;
  routedTo: string;
  status: string;
  generatedComplaintText: string;
  createdAt: string;
}

export interface TransactionView {
  id: number;
  category: string;
  amount: number;
  merchant: string;
  isBnpl: boolean;
  occurredAt: string;
}

export interface BudgetSummary {
  month?: string;
  monthlyLimit: number;
  spentThisSet: number;
  remaining: number;
  percentUsed: number;
  nudge: string;
}

export interface LiteracyContentView {
  id: number;
  title: string;
  body: string;
  topic: string;
  language: string;
  format: string;
}

// ---- banking ------------------------------------------------------------------------

export type AccountType = 'SAVINGS' | 'CURRENT';
export type TransferMode = 'IMPS' | 'NEFT' | 'RTGS';

export interface BankAccountView {
  id: number;
  accountNumber: string;
  ifsc: string;
  type: AccountType;
  status: 'ACTIVE' | 'FROZEN';
  balance: number;
  dailyTransferLimit: number;
  createdAt: string;
}

export interface LedgerEntryView {
  id: number;
  type: 'CREDIT' | 'DEBIT';
  channel: string;
  amount: number;
  balanceAfter: number;
  description: string;
  reference: string;
  counterparty: string | null;
  createdAt: string;
}

export interface StatementView {
  accountId: number;
  accountNumber: string;
  from: string;
  to: string;
  entries: LedgerEntryView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BeneficiaryView {
  id: number;
  accountNumber: string;
  ifsc: string;
  maskedName: string;
  nickname: string | null;
  createdAt: string;
}

export interface TransferCheckResponse {
  payeeMaskedName: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  reasons: string[];
  checklistPrompts: string[];
  requiresAcknowledgement: boolean;
  blocked: boolean;
}

export interface TransferView {
  reference: string;
  fromAccountId: number;
  toAccountNumber: string;
  toMaskedName: string;
  amount: number;
  mode: TransferMode;
  remark: string | null;
  status: string;
  createdAt: string;
}

export interface FdQuote {
  principal: number;
  tenureMonths: number;
  ratePercent: number;
  maturityAmount: number;
  interestEarned: number;
  maturityDate: string;
}

export interface FdView {
  id: number;
  accountId: number;
  principal: number;
  ratePercent: number;
  tenureMonths: number;
  maturityAmount: number;
  startDate: string;
  maturityDate: string;
  status: 'ACTIVE' | 'MATURED' | 'CLOSED_PREMATURE';
  payoutAmount: number | null;
  closedAt: string | null;
}

export interface EmiInstallment {
  number: number;
  emi: number;
  principalPart: number;
  interestPart: number;
  balance: number;
}

export interface EmiResponse {
  principal: number;
  annualRatePercent: number;
  tenureMonths: number;
  emi: number;
  totalInterest: number;
  totalPayable: number;
  schedule: EmiInstallment[];
}
