export type Role = 'CUSTOMER' | 'FRAUD_ANALYST' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
export type AccountType = 'SAVINGS' | 'CHECKING';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type TransactionType = 'DEPOSIT' | 'TRANSFER_IN' | 'TRANSFER_OUT';
export type TransactionStatus = 'APPROVED_SIMULATED' | 'PENDING_REVIEW' | 'BLOCKED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type AlertStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED_RELEASED' | 'RESOLVED_BLOCKED';
export type NotificationType =
  | 'TRANSFER_RECEIVED'
  | 'TRANSFER_SENT'
  | 'TRANSFER_HELD'
  | 'TRANSFER_BLOCKED'
  | 'TRANSFER_RELEASED'
  | 'ACCOUNT_FROZEN'
  | 'ACCOUNT_UNFROZEN'
  | 'ACCOUNT_CREATED'
  | 'SECURITY_ALERT';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  role: Role;
  status: UserStatus;
  failedLoginAttempts?: number;
  lockoutUntil?: string | null;
  lastLoginAt?: string | null;
  createdAt?: string;
  accounts?: Account[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Account {
  id: number;
  accountNumber: string;
  maskedAccountNumber: string;
  accountType: AccountType;
  currency: string;
  balance: number;
  availableBalance: number;
  heldAmount: number;
  status: AccountStatus;
  dailyLimit: number;
  createdAt: string;
}

export interface Beneficiary {
  id: number;
  nickname: string;
  recipientAccountId: number;
  recipientAccountNumber: string;
  recipientMaskedAccountNumber: string;
  recipientName: string;
  createdAt: string;
}

export interface Transaction {
  id: number;
  referenceId: string;
  sourceAccountId: number;
  sourceMaskedAccountNumber: string;
  destinationAccountId: number;
  destinationMaskedAccountNumber: string;
  type: TransactionType;
  amount: number;
  status: TransactionStatus;
  failureReason?: string;
  idempotencyKey?: string;
  createdAt: string;
  completedAt?: string;
}

export interface FraudAlertHit {
  id: number;
  ruleCode: string;
  ruleName: string;
  scoreContribution: number;
  reasonExplanation: string;
}

export interface FraudAlert {
  id: number;
  transaction: Transaction;
  riskScore: number;
  riskLevel: RiskLevel;
  status: AlertStatus;
  assignedTo?: UserSummary;
  decisionNotes?: string;
  createdAt: string;
  resolvedAt?: string;
  hits?: FraudAlertHit[];
}

export interface FraudRule {
  id: number;
  ruleCode: string;
  name: string;
  description: string;
  points: number;
  scorePoints?: number;
  isEnabled: boolean;
  parametersJson?: string;
  updatedAt: string;
}

export interface SystemSetting {
  id: number;
  settingKey: string;
  settingValue: string;
  description: string;
  updatedAt: string;
}

export interface AuditLog {
  id: number;
  actorUserId?: number;
  actorName?: string;
  actorEmail?: string;
  action: string;
  targetType: string;
  targetId: string;
  detailsJson: string;
  ipAddress?: string;
  requestId?: string;
  createdAt: string;
}

export interface AdminDashboardMetrics {
  totalUsers: number;
  activeUsers: number;
  totalAccounts: number;
  totalBalance: number;
  totalTransactions: number;
  approvedTransactions: number;
  heldTransactions: number;
  blockedTransactions: number;
  openAlerts: number;
  activeRules: number;
}

export interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  readAt?: string | null;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
