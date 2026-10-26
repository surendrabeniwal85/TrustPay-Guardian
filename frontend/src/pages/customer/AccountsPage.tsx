import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../../api/axiosClient';
import { useToast } from '../../context/ToastContext';
import { Account, Transaction, ApiResponse, Page } from '../../types';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';

export const AccountsPage: React.FC = () => {
  const { showToast } = useToast();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [statementTransactions, setStatementTransactions] = useState<Transaction[]>([]);
  const [isLoadingAccounts, setIsLoadingAccounts] = useState<boolean>(true);
  const [isLoadingStatement, setIsLoadingStatement] = useState<boolean>(false);
  const [accountsError, setAccountsError] = useState<string | null>(null);

  // Statement Filters & Pagination
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Open New Account Modal
  const [showOpenModal, setShowOpenModal] = useState<boolean>(false);
  const [newAccountType, setNewAccountType] = useState<'SAVINGS' | 'CHECKING'>('SAVINGS');
  const [isOpening, setIsOpening] = useState<boolean>(false);

  const fetchAccounts = useCallback(async () => {
    setIsLoadingAccounts(true);
    setAccountsError(null);
    try {
      const res = await api.get<ApiResponse<Account[]>>('/accounts');
      const data = res.data?.data || [];
      setAccounts(data);
      if (data.length > 0 && !selectedAccountId) {
        setSelectedAccountId(data[0].id);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch customer accounts';
      setAccountsError(msg);
      showToast(msg, 'danger');
    } finally {
      setIsLoadingAccounts(false);
    }
  }, [selectedAccountId, showToast]);

  const fetchStatement = useCallback(async () => {
    if (!selectedAccountId) return;
    setIsLoadingStatement(true);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);

      const res = await api.get<ApiResponse<Page<Transaction>>>(
        `/accounts/${selectedAccountId}/statement?${params.toString()}`
      );
      const data = res.data?.data;
      if (data) {
        setStatementTransactions(data.content || []);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      } else {
        setStatementTransactions([]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unable to load account statement';
      showToast(msg, 'danger');
    } finally {
      setIsLoadingStatement(false);
    }
  }, [selectedAccountId, page, size, startDate, endDate, showToast]);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  useEffect(() => {
    if (selectedAccountId) {
      fetchStatement();
    }
  }, [selectedAccountId, fetchStatement]);

  const handleOpenAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (accounts.length >= 3) {
      showToast('Maximum 3 accounts limit reached for this customer profile', 'warning');
      setShowOpenModal(false);
      return;
    }

    setIsOpening(true);
    try {
      const res = await api.post<ApiResponse<Account>>('/accounts', {
        accountType: newAccountType,
      });
      showToast(`Account ${res.data?.data?.maskedAccountNumber || ''} opened successfully!`, 'success');
      setShowOpenModal(false);
      await fetchAccounts();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to open account';
      showToast(msg, 'danger');
    } finally {
      setIsOpening(false);
    }
  };

  // PRD Section 11 [S] CSV Export
  const handleExportCsv = () => {
    if (statementTransactions.length === 0) {
      showToast('No statement records to export', 'info');
      return;
    }

    const headers = [
      'Transaction Reference',
      'Date (UTC)',
      'Type',
      'Amount (INR)',
      'Status',
      'Source Account',
      'Destination Account',
    ];

    const rows = statementTransactions.map((tx) => [
      `"${tx.referenceId}"`,
      `"${new Date(tx.createdAt).toISOString()}"`,
      `"${tx.type}"`,
      tx.amount.toFixed(2),
      `"${tx.status}"`,
      `"${tx.sourceMaskedAccountNumber || ''}"`,
      `"${tx.destinationMaskedAccountNumber || ''}"`,
    ]);

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    const selectedAcc = accounts.find((a) => a.id === selectedAccountId);
    const accNum = selectedAcc ? selectedAcc.accountNumber : 'statement';
    link.setAttribute('download', `statement-${accNum}-${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    showToast('Statement CSV exported successfully', 'success');
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

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Accounts &amp; Statements</h1>
          <p className="text-secondary small mb-0">
            View simulated account balances, daily limits, and filterable transaction statements.
          </p>
        </div>
        <div>
          <button
            onClick={() => setShowOpenModal(true)}
            disabled={accounts.length >= 3}
            className="btn btn-primary btn-sm d-flex align-items-center gap-2"
            title={accounts.length >= 3 ? 'Maximum of 3 accounts allowed' : 'Open another account'}
          >
            <i className="bi bi-plus-lg"></i>
            <span>Open New Account ({accounts.length}/3)</span>
          </button>
        </div>
      </div>

      {/* Account Cards Section */}
      {isLoadingAccounts ? (
        <div className="row g-3 mb-4">
          <div className="col-md-4"><Skeleton height="9rem" /></div>
          <div className="col-md-4"><Skeleton height="9rem" /></div>
          <div className="col-md-4"><Skeleton height="9rem" /></div>
        </div>
      ) : accountsError ? (
        <div className="mb-4">
          <EmptyState
            icon="bi-exclamation-triangle"
            title="Failed to Load Accounts"
            description={accountsError}
            action={
              <button onClick={fetchAccounts} className="btn btn-outline-primary btn-sm">
                Try Again
              </button>
            }
          />
        </div>
      ) : (
        <div className="row g-3 mb-4">
          {accounts.map((acc) => {
            const isSelected = acc.id === selectedAccountId;
            return (
              <div key={acc.id} className="col-md-4">
                <div
                  className={`card h-100 border cursor-pointer transition-all ${
                    isSelected ? 'border-primary shadow' : 'border-secondary-subtle'
                  }`}
                  style={{
                    backgroundColor: 'var(--tpg-bg-surface)',
                    cursor: 'pointer',
                    boxShadow: isSelected ? '0 0 0 2px var(--tpg-primary)' : undefined,
                  }}
                  onClick={() => {
                    setSelectedAccountId(acc.id);
                    setPage(0);
                  }}
                >
                  <div className="card-body">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="badge bg-primary-subtle text-primary border border-primary-subtle">
                        {acc.accountType}
                      </span>
                      <span className={`badge ${acc.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}`}>
                        {acc.status}
                      </span>
                    </div>

                    <div className="text-secondary small">Account Number</div>
                    <div className="h6 font-monospace fw-bold mb-3">{acc.maskedAccountNumber}</div>

                    <div className="row g-2 pt-2 border-top">
                      <div className="col-6">
                        <div className="text-secondary small" style={{ fontSize: '0.75rem' }}>Ledger Balance</div>
                        <div className="fw-bold">₹ {acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
                      </div>
                      <div className="col-6 text-end">
                        <div className="text-secondary small" style={{ fontSize: '0.75rem' }}>Available</div>
                        <div className="fw-bold text-success">
                          ₹ {acc.availableBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </div>
                      </div>
                    </div>

                    {acc.heldAmount > 0 && (
                      <div className="mt-2 p-1 px-2 rounded bg-warning-subtle text-warning-emphasis small d-flex justify-content-between" style={{ fontSize: '0.75rem' }}>
                        <span>Held Amount:</span>
                        <span className="fw-bold">₹ {acc.heldAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Account Statement Section */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-header bg-transparent py-3">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div className="d-flex align-items-center gap-2">
              <i className="bi bi-file-earmark-spreadsheet text-primary fs-5"></i>
              <h2 className="h5 fw-bold mb-0">Account Statement</h2>
              {selectedAccountId && (
                <span className="badge bg-secondary-subtle text-secondary font-monospace">
                  {accounts.find((a) => a.id === selectedAccountId)?.maskedAccountNumber}
                </span>
              )}
            </div>

            {/* Filter Controls & CSV Button */}
            <div className="d-flex flex-wrap align-items-center gap-2">
              <div className="d-flex align-items-center gap-1">
                <label className="small text-secondary mb-0">From:</label>
                <input
                  type="date"
                  className="form-control form-control-sm"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setPage(0);
                  }}
                />
              </div>

              <div className="d-flex align-items-center gap-1">
                <label className="small text-secondary mb-0">To:</label>
                <input
                  type="date"
                  className="form-control form-control-sm"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setPage(0);
                  }}
                />
              </div>

              <select
                className="form-select form-select-sm"
                style={{ width: '90px' }}
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                <option value={10}>10 / pg</option>
                <option value={25}>25 / pg</option>
                <option value={50}>50 / pg</option>
              </select>

              <button
                onClick={handleExportCsv}
                disabled={statementTransactions.length === 0}
                className="btn btn-outline-success btn-sm d-flex align-items-center gap-1"
                title="Export statement as CSV"
              >
                <i className="bi bi-download"></i>
                <span>[S] CSV Export</span>
              </button>
            </div>
          </div>
        </div>

        <div className="card-body p-0">
          {isLoadingStatement ? (
            <div className="p-4">
              <Skeleton height="2rem" className="mb-2" />
              <Skeleton height="2rem" className="mb-2" />
              <Skeleton height="2rem" className="mb-2" />
              <Skeleton height="2rem" />
            </div>
          ) : statementTransactions.length === 0 ? (
            <div className="p-5 text-center">
              <EmptyState
                icon="bi-journal-x"
                title="No Statement Entries Found"
                description={
                  startDate || endDate
                    ? 'No transactions match the selected date filters for this account.'
                    : 'This account has no recorded transactions yet.'
                }
              />
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light small text-secondary">
                  <tr>
                    <th className="ps-3">Reference</th>
                    <th>Date &amp; Time</th>
                    <th>Type</th>
                    <th>Counterparty Account</th>
                    <th>Amount</th>
                    <th className="pe-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {statementTransactions.map((tx) => (
                    <tr key={tx.id}>
                      <td className="ps-3 font-monospace small">{tx.referenceId}</td>
                      <td className="small text-muted">
                        {new Date(tx.createdAt).toLocaleDateString([], {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                      <td>
                        <span className="badge bg-secondary-subtle text-secondary">{tx.type}</span>
                      </td>
                      <td className="font-monospace small">
                        {tx.type === 'TRANSFER_OUT'
                          ? tx.destinationMaskedAccountNumber || 'External'
                          : tx.sourceMaskedAccountNumber || 'Self Deposit'}
                      </td>
                      <td className={`fw-semibold ${tx.type === 'TRANSFER_OUT' ? 'text-danger' : 'text-success'}`}>
                        {tx.type === 'TRANSFER_OUT' ? '-' : '+'} ₹{' '}
                        {tx.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="pe-3">{getStatusBadge(tx.status)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="card-footer bg-transparent d-flex justify-content-between align-items-center py-2 px-3">
            <span className="text-secondary small">
              Showing page {page + 1} of {totalPages} ({totalElements} total entries)
            </span>
            <div className="btn-group btn-group-sm">
              <button
                className="btn btn-outline-secondary"
                disabled={page === 0 || isLoadingStatement}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <i className="bi bi-chevron-left"></i> Previous
              </button>
              <button
                className="btn btn-outline-secondary"
                disabled={page >= totalPages - 1 || isLoadingStatement}
                onClick={() => setPage((p) => p + 1)}
              >
                Next <i className="bi bi-chevron-right"></i>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Open Account Modal */}
      {showOpenModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Open Additional Simulated Account</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowOpenModal(false)}
                  disabled={isOpening}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleOpenAccount}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Account Type</label>
                    <select
                      className="form-select"
                      value={newAccountType}
                      onChange={(e) => setNewAccountType(e.target.value as 'SAVINGS' | 'CHECKING')}
                    >
                      <option value="SAVINGS">SAVINGS Account (Standard interest-bearing)</option>
                      <option value="CHECKING">CHECKING Account (Daily operations &amp; transfers)</option>
                    </select>
                  </div>

                  <div className="p-3 rounded bg-body-tertiary small text-secondary">
                    <i className="bi bi-info-circle me-1 text-primary"></i>
                    Each customer can hold up to 3 active simulated accounts. New accounts are created with zero initial balance and a ₹1,00,000.00 daily transfer limit.
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowOpenModal(false)}
                    disabled={isOpening}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                    disabled={isOpening}
                  >
                    {isOpening && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Create Account</span>
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
