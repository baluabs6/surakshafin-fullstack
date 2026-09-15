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
