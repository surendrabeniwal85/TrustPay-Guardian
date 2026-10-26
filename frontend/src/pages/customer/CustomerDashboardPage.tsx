import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/axiosClient';
import { Account, Transaction, ApiResponse, Page } from '../../types';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';

interface MonthlyCashflow {
  month: string;
  income: number;
  spending: number;
}

export const CustomerDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Deposit modal state
  const [showDepositModal, setShowDepositModal] = useState<boolean>(false);
  const [selectedDepositAccountId, setSelectedDepositAccountId] = useState<number | ''>('');
  const [depositAmount, setDepositAmount] = useState<string>('5000');
  const [isDepositing, setIsDepositing] = useState<boolean>(false);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [accRes, txnRes] = await Promise.all([
        api.get<ApiResponse<Account[]>>('/accounts'),
        api.get<ApiResponse<Page<Transaction>>>('/transactions?page=0&size=5'),
      ]);

      const fetchedAccounts = accRes.data?.data || [];
      setAccounts(fetchedAccounts);
      if (fetchedAccounts.length > 0) {
        setSelectedDepositAccountId(fetchedAccounts[0].id);
      }

      const txnPage = txnRes.data?.data;
      setRecentTransactions(txnPage?.content || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unable to load banking overview';
      setError(msg);
      showToast(msg, 'danger');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const totalBalance = accounts.reduce((acc, a) => acc + (a.balance || 0), 0);
  const totalAvailable = accounts.reduce((acc, a) => acc + (a.availableBalance || 0), 0);
  const totalHeld = accounts.reduce((acc, a) => acc + (a.heldAmount || 0), 0);

  // Compute 6-month simulated cashflow data
  const generateCashflowData = (): MonthlyCashflow[] => {
    const months = ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'];
    return months.map((month, idx) => {
      // Dynamic baseline proportional to current total balance
      const baseIncome = Math.round(Math.max(15000, totalBalance * 0.25) * (0.8 + idx * 0.05));
      const baseSpending = Math.round(baseIncome * (0.55 + (idx % 3) * 0.1));
      return {
        month,
        income: baseIncome,
        spending: baseSpending,
      };
    });
  };

  const cashflowData = generateCashflowData();

  const handleDepositSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDepositAccountId) {
      showToast('Please select an account for deposit', 'warning');
      return;
    }
    const amountNum = parseFloat(depositAmount);
    if (isNaN(amountNum) || amountNum <= 0) {
      showToast('Please enter a valid deposit amount', 'warning');
      return;
    }
    if (amountNum > 50000) {
      showToast('Maximum single deposit is ₹50,000.00', 'warning');
      return;
    }

    setIsDepositing(true);
    try {
      await api.post('/transactions/deposit', {
        destinationAccountId: Number(selectedDepositAccountId),
        amount: amountNum,
      });
      showToast(`Successfully deposited ₹${amountNum.toLocaleString('en-IN')}`, 'success');
      setShowDepositModal(false);
      setDepositAmount('5000');
      fetchData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Deposit failed';
      showToast(msg, 'danger');
    } finally {
      setIsDepositing(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED_SIMULATED':
        return <span className="badge bg-success-subtle text-success border border-success-subtle">Completed</span>;
      case 'PENDING_REVIEW':
        return <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">Held for Review</span>;
      case 'BLOCKED':
        return <span className="badge bg-danger-subtle text-danger border border-danger-subtle">Blocked</span>;
      default:
        return <span className="badge bg-secondary">{status}</span>;
    }
  };

  if (isLoading) {
    return (
      <div className="container py-4">
        <Skeleton height="3rem" width="40%" className="mb-4" />
        <div className="row g-4 mb-4">
          <div className="col-md-4"><Skeleton height="8rem" /></div>
          <div className="col-md-4"><Skeleton height="8rem" /></div>
          <div className="col-md-4"><Skeleton height="8rem" /></div>
        </div>
        <Skeleton height="20rem" className="mb-4" />
        <Skeleton height="15rem" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="container py-5">
        <EmptyState
          icon="bi-exclamation-triangle"
          title="Could Not Load Dashboard"
          description={error}
          action={
            <button onClick={fetchData} className="btn btn-outline-primary btn-sm">
              Retry
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Welcome Greeting & Quick Actions */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">
            Welcome back, {user?.fullName || 'Customer'}
          </h1>
          <p className="text-secondary small mb-0">
            Simulated Digital Banking &bull; Monitored by Real-Time Fraud Engine
          </p>
        </div>
        <div className="d-flex align-items-center gap-2">
          <Link to="/app/transfer" className="btn btn-primary btn-sm d-flex align-items-center gap-2">
            <i className="bi bi-arrow-up-right-circle"></i>
            <span>Send Money</span>
          </Link>
          <button
            onClick={() => setShowDepositModal(true)}
            className="btn btn-outline-success btn-sm d-flex align-items-center gap-2"
          >
            <i className="bi bi-plus-circle"></i>
            <span>Add Funds</span>
          </button>
          <Link to="/app/beneficiaries" className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-2">
            <i className="bi bi-person-plus"></i>
            <span>Add Beneficiary</span>
          </Link>
        </div>
      </div>

      {/* Overview Balance Cards */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="text-secondary small fw-medium">Total Ledger Balance</span>
                <span className="badge bg-primary-subtle text-primary border border-primary-subtle">
                  {accounts.length} {accounts.length === 1 ? 'Account' : 'Accounts'}
                </span>
              </div>
              <h2 className="display-6 fw-bold text-primary mb-1">
                ₹ {totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </h2>
              <div className="small text-muted">Combined across all open accounts</div>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="text-secondary small fw-medium">Available Balance</span>
                <i className="bi bi-check-circle text-success fs-5"></i>
              </div>
              <h2 className="display-6 fw-bold text-success mb-1">
                ₹ {totalAvailable.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </h2>
              <div className="small text-muted">Ready for transfers or payments</div>
            </div>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="text-secondary small fw-medium">Reserved / Held Amount</span>
                {totalHeld > 0 ? (
                  <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                    Pending Review
                  </span>
                ) : (
                  <i className="bi bi-shield-check text-muted fs-5"></i>
                )}
              </div>
              <h2 className={`display-6 fw-bold mb-1 ${totalHeld > 0 ? 'text-warning-emphasis' : 'text-secondary'}`}>
                ₹ {totalHeld.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </h2>
              <div className="small text-muted">
                {totalHeld > 0 ? 'Held pending fraud analyst review' : 'No funds currently held'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Account Cards Carousel/Grid */}
      <div className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h2 className="h5 fw-bold mb-0">Your Accounts</h2>
          <Link to="/app/accounts" className="small text-decoration-none text-primary fw-medium">
            Manage & Statement <i className="bi bi-arrow-right"></i>
          </Link>
        </div>
        <div className="row g-3">
          {accounts.map((acc) => (
            <div key={acc.id} className="col-md-6 col-lg-4">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <span className="badge bg-secondary-subtle text-secondary border">
                      {acc.accountType}
                    </span>
                    <span className={`badge ${acc.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}`}>
                      {acc.status}
                    </span>
                  </div>
                  <div className="text-secondary small mb-1">Account Number</div>
                  <div className="h6 font-monospace mb-3">{acc.maskedAccountNumber}</div>

                  <div className="d-flex justify-content-between align-items-end pt-2 border-top">
                    <div>
                      <div className="text-secondary small">Balance</div>
                      <div className="fw-bold fs-5">
                        ₹ {acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                    </div>
                    <div className="text-end">
                      <div className="text-secondary small">Daily Limit</div>
                      <div className="small fw-semibold text-muted">
                        ₹ {acc.dailyLimit.toLocaleString('en-IN')}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Income vs Spending 6-Month Chart */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <div>
              <h2 className="h5 fw-bold mb-1">Cashflow Analytics (6 Months)</h2>
              <p className="text-secondary small mb-0">Monthly comparison of inflows versus outgoing transfers</p>
            </div>
            <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">
              Simulated Overview
            </span>
          </div>

          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={cashflowData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" opacity={0.15} />
                <XAxis dataKey="month" stroke="var(--tpg-text-secondary)" fontSize={12} />
                <YAxis
                  stroke="var(--tpg-text-secondary)"
                  fontSize={12}
                  tickFormatter={(val) => `₹${(val / 1000).toFixed(0)}k`}
                />
                <Tooltip
                  formatter={(val) => [`₹ ${Number(val ?? 0).toLocaleString('en-IN')}`, '']}
                  contentStyle={{
                    backgroundColor: 'var(--tpg-bg-surface)',
                    borderColor: 'var(--tpg-border-color)',
                    borderRadius: '8px',
                    color: 'var(--tpg-text-primary)',
                  }}
                />
                <Legend />
                <Bar dataKey="income" name="Inflow (Deposits & Credits)" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar dataKey="spending" name="Outflow (Transfers)" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Recent Transactions & Security Card */}
      <div className="row g-4">
        {/* Recent Transactions */}
        <div className="col-lg-8">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-header bg-transparent d-flex justify-content-between align-items-center py-3">
              <h2 className="h5 fw-bold mb-0">Recent Activity</h2>
              <Link to="/app/history" className="small text-decoration-none text-primary fw-medium">
                View Full History <i className="bi bi-arrow-right"></i>
              </Link>
            </div>
            <div className="card-body p-0">
              {recentTransactions.length === 0 ? (
                <div className="p-4 text-center">
                  <EmptyState
                    icon="bi-receipt-cutoff"
                    title="No Transactions Yet"
                    description="Your recent deposits and transfers will appear here."
                  />
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light small text-secondary">
                      <tr>
                        <th className="ps-3">Reference</th>
                        <th>Type</th>
                        <th>Status</th>
                        <th>Amount</th>
                        <th className="pe-3 text-end">Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentTransactions.map((tx) => (
                        <tr key={tx.id}>
                          <td className="ps-3 font-monospace small">{tx.referenceId}</td>
                          <td>
                            <span className="badge bg-secondary-subtle text-secondary">
                              {tx.type}
                            </span>
                          </td>
                          <td>{getStatusBadge(tx.status)}</td>
                          <td className="fw-semibold">
                            ₹ {tx.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                          </td>
                          <td className="pe-3 text-end text-muted small">
                            {new Date(tx.createdAt).toLocaleDateString([], {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Security & System Status Card */}
        <div className="col-lg-4">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-header bg-transparent py-3">
              <h2 className="h5 fw-bold mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-shield-check text-primary"></i>
                <span>Account Protection</span>
              </h2>
            </div>
            <div className="card-body">
              <div className="d-flex align-items-center justify-content-between p-3 rounded bg-success-subtle border border-success-subtle mb-3">
                <div className="d-flex align-items-center gap-2">
                  <i className="bi bi-shield-fill-check text-success fs-4"></i>
                  <div>
                    <div className="fw-bold text-success small">Fraud Engine Active</div>
                    <div className="text-secondary small" style={{ fontSize: '0.75rem' }}>
                      7-rule real-time heuristics
                    </div>
                  </div>
                </div>
                <span className="badge bg-success">SECURED</span>
              </div>

              <ul className="list-group list-group-flush small mb-3">
                <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                  <span className="text-secondary">Assigned Role:</span>
                  <span className="fw-semibold">{user?.role || 'CUSTOMER'}</span>
                </li>
                <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                  <span className="text-secondary">Email:</span>
                  <span className="fw-semibold text-truncate" style={{ maxWidth: '160px' }}>
                    {user?.email}
                  </span>
                </li>
                <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                  <span className="text-secondary">Token Storage:</span>
                  <span className="badge bg-info-subtle text-info-emphasis">In-Memory (XSS Safe)</span>
                </li>
                <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                  <span className="text-secondary">Last Login:</span>
                  <span className="text-muted">
                    {user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Active Session'}
                  </span>
                </li>
              </ul>

              <div className="p-2 rounded bg-body-tertiary small text-secondary">
                <i className="bi bi-info-circle me-1"></i>
                TrustPay Guardian operates under educational simulation rules. Held transactions are safely queued for manual fraud analyst triage.
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Demo Deposit Modal */}
      {showDepositModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Deposit Simulated Funds</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowDepositModal(false)}
                  disabled={isDepositing}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleDepositSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Destination Account</label>
                    <select
                      className="form-select"
                      value={selectedDepositAccountId}
                      onChange={(e) => setSelectedDepositAccountId(Number(e.target.value))}
                      required
                    >
                      {accounts.map((acc) => (
                        <option key={acc.id} value={acc.id}>
                          {acc.accountType} - {acc.maskedAccountNumber} (Bal: ₹{acc.balance.toLocaleString('en-IN')})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Deposit Amount (₹)</label>
                    <input
                      type="number"
                      step="1"
                      min="1"
                      max="50000"
                      className="form-control"
                      value={depositAmount}
                      onChange={(e) => setDepositAmount(e.target.value)}
                      required
                    />
                    <div className="form-text small">Maximum simulated deposit limit: ₹50,000.00</div>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowDepositModal(false)}
                    disabled={isDepositing}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-success btn-sm d-flex align-items-center gap-2"
                    disabled={isDepositing}
                  >
                    {isDepositing && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Confirm Deposit</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
