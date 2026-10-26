import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../../api/axiosClient';
import { useToast } from '../../context/ToastContext';
import { Transaction, ApiResponse, Page } from '../../types';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';

export const TransactionsPage: React.FC = () => {
  const { showToast } = useToast();

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Detail Drawer State
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  const fetchTransactions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      if (statusFilter) params.append('status', statusFilter);
      if (typeFilter) params.append('type', typeFilter);
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);
      if (search.trim()) params.append('search', search.trim());

      const res = await api.get<ApiResponse<Page<Transaction>>>(`/transactions?${params.toString()}`);
      const data = res.data?.data;
      if (data) {
        setTransactions(data.content || []);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      } else {
        setTransactions([]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch transaction history';
      setError(msg);
      showToast(msg, 'danger');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, statusFilter, typeFilter, startDate, endDate, search, showToast]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  const handleResetFilters = () => {
    setStatusFilter('');
    setTypeFilter('');
    setStartDate('');
    setEndDate('');
    setSearch('');
    setPage(0);
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
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1">Transaction History</h1>
        <p className="text-secondary small mb-0">
          Search, filter, and inspect detailed audit trails for every deposit and transfer.
        </p>
      </div>

      {/* Filter Toolbar */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body">
          <div className="row g-2 align-items-center">
            {/* Search */}
            <div className="col-md-3">
              <div className="input-group input-group-sm">
                <span className="input-group-text"><i className="bi bi-search"></i></span>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search reference..."
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setPage(0);
                  }}
                />
              </div>
            </div>

            {/* Status Filter */}
            <div className="col-6 col-md-2">
              <select
                className="form-select form-select-sm"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Statuses</option>
                <option value="APPROVED_SIMULATED">Completed</option>
                <option value="PENDING_REVIEW">Held for Review</option>
                <option value="BLOCKED">Blocked</option>
              </select>
            </div>

            {/* Type Filter */}
            <div className="col-6 col-md-2">
              <select
                className="form-select form-select-sm"
                value={typeFilter}
                onChange={(e) => {
                  setTypeFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Types</option>
                <option value="DEPOSIT">Deposit</option>
                <option value="TRANSFER_OUT">Transfer Out</option>
                <option value="TRANSFER_IN">Transfer In</option>
              </select>
            </div>

            {/* Date Range */}
            <div className="col-6 col-md-2">
              <input
                type="date"
                className="form-control form-control-sm"
                placeholder="From Date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <div className="col-6 col-md-2">
              <input
                type="date"
                className="form-control form-control-sm"
                placeholder="To Date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value);
                  setPage(0);
                }}
              />
            </div>

            {/* Reset */}
            <div className="col-md-1 text-end">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm w-100"
                onClick={handleResetFilters}
                title="Reset all filters"
              >
                Reset
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Transactions Table */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body p-0">
          {isLoading ? (
            <div className="p-4">
              <Skeleton height="2.5rem" className="mb-2" />
              <Skeleton height="2.5rem" className="mb-2" />
              <Skeleton height="2.5rem" className="mb-2" />
              <Skeleton height="2.5rem" />
            </div>
          ) : error ? (
            <div className="p-4">
              <EmptyState
                icon="bi-exclamation-triangle"
                title="Failed to Load History"
                description={error}
                action={
                  <button onClick={fetchTransactions} className="btn btn-outline-primary btn-sm">
                    Try Again
                  </button>
                }
              />
            </div>
          ) : transactions.length === 0 ? (
            <div className="p-5 text-center">
              <EmptyState
                icon="bi-clock-history"
                title="No Transactions Found"
                description={
                  search || statusFilter || typeFilter || startDate || endDate
                    ? 'No transactions match the selected filter criteria. Try clearing some filters.'
                    : 'You have not made or received any transactions yet.'
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
                    <th>Counterparty</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th className="pe-3 text-end">Details</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr
                      key={tx.id}
                      onClick={() => setSelectedTx(tx)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td className="ps-3 font-monospace small fw-semibold text-primary">
                        {tx.referenceId}
                      </td>
                      <td className="small text-muted">
                        {new Date(tx.createdAt).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
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
                      <td>{getStatusBadge(tx.status)}</td>
                      <td className="pe-3 text-end">
                        <button
                          className="btn btn-sm btn-link p-0 text-decoration-none text-primary"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedTx(tx);
                          }}
                        >
                          View <i className="bi bi-chevron-right"></i>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="card-footer bg-transparent d-flex justify-content-between align-items-center py-2 px-3">
            <span className="text-secondary small">
              Page {page + 1} of {totalPages} ({totalElements} transactions)
            </span>
            <div className="d-flex align-items-center gap-2">
              <select
                className="form-select form-select-sm"
                style={{ width: '85px' }}
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
              <div className="btn-group btn-group-sm">
                <button
                  className="btn btn-outline-secondary"
                  disabled={page === 0 || isLoading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  <i className="bi bi-chevron-left"></i> Previous
                </button>
                <button
                  className="btn btn-outline-secondary"
                  disabled={page >= totalPages - 1 || isLoading}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next <i className="bi bi-chevron-right"></i>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Transaction Detail Drawer / Offcanvas Modal */}
      {selectedTx && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <div>
                  <h5 className="modal-title fw-bold mb-0">Transaction Forensic Details</h5>
                  <span className="font-monospace text-secondary small">{selectedTx.referenceId}</span>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedTx(null)}
                  aria-label="Close"
                ></button>
              </div>

              <div className="modal-body">
                {/* Main Status & Amount Banner */}
                <div className="text-center p-3 rounded bg-body-tertiary mb-3 border">
                  <div className="text-secondary small mb-1">Transaction Amount</div>
                  <div className="display-6 fw-bold mb-2">
                    ₹ {selectedTx.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                  <div>{getStatusBadge(selectedTx.status)}</div>
                </div>

                {/* Key Attributes */}
                <ul className="list-group list-group-flush small mb-3">
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Type:</span>
                    <span className="fw-semibold">{selectedTx.type}</span>
                  </li>
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Source Account:</span>
                    <span className="font-monospace fw-semibold">
                      {selectedTx.sourceMaskedAccountNumber || 'Self Deposit'}
                    </span>
                  </li>
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Destination Account:</span>
                    <span className="font-monospace fw-semibold">
                      {selectedTx.destinationMaskedAccountNumber || 'Internal Deposit'}
                    </span>
                  </li>
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Initiated At:</span>
                    <span className="text-muted">
                      {new Date(selectedTx.createdAt).toLocaleString()}
                    </span>
                  </li>
                  {selectedTx.completedAt && (
                    <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                      <span className="text-secondary">Completed At:</span>
                      <span className="text-muted">
                        {new Date(selectedTx.completedAt).toLocaleString()}
                      </span>
                    </li>
                  )}
                  {selectedTx.failureReason && (
                    <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                      <span className="text-secondary">Status Note:</span>
                      <span className="text-danger fw-semibold">{selectedTx.failureReason}</span>
                    </li>
                  )}
                </ul>

                {/* Security Context Info */}
                <div className="p-3 rounded bg-body-tertiary small text-secondary border">
                  <div className="fw-semibold mb-1 text-body d-flex align-items-center gap-1">
                    <i className="bi bi-shield-lock text-primary"></i>
                    <span>Banking Security Assurance</span>
                  </div>
                  {selectedTx.status === 'APPROVED_SIMULATED' && (
                    <div>Risk score was below the dynamic 40-point threshold. Immediate ledger settlement completed.</div>
                  )}
                  {selectedTx.status === 'PENDING_REVIEW' && (
                    <div>Risk score fell in the 40–69 review band. Funds are safely held on ledger pending fraud analyst triage.</div>
                  )}
                  {selectedTx.status === 'BLOCKED' && (
                    <div>Risk score breached the 70-point threshold. Transaction aborted without moving any account funds.</div>
                  )}
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setSelectedTx(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
