import React, { useState, useEffect, useCallback } from 'react';
import { api, getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Account, Beneficiary, Transaction, ApiResponse } from '../types';

export const CustomerDashboard: React.FC = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [txLoading, setTxLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Transfer Form State
  const [recipientAccountNum, setRecipientAccountNum] = useState<string>('');
  const [transferAmount, setTransferAmount] = useState<string>('');
  const [transferNote, setTransferNote] = useState<string>('');
  const [lastTransferResult, setLastTransferResult] = useState<Transaction | null>(null);

  // Modals
  const [showDepositModal, setShowDepositModal] = useState<boolean>(false);
  const [depositAmount, setDepositAmount] = useState<string>('5000');
  const [depositLoading, setDepositLoading] = useState<boolean>(false);

  const [showBeneficiaryModal, setShowBeneficiaryModal] = useState<boolean>(false);
  const [bNickname, setBNickname] = useState<string>('');
  const [bAccountNum, setBAccountNum] = useState<string>('');
  const [bLoading, setBLoading] = useState<boolean>(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [accRes, benRes] = await Promise.all([
        api.get<ApiResponse<Account[]>>('/accounts'),
        api.get<ApiResponse<Beneficiary[]>>('/beneficiaries'),
      ]);

      const accs = accRes.data.data || [];
      setAccounts(accs);
      setBeneficiaries(benRes.data.data || []);

      if (accs.length > 0) {
        const activeAcc = selectedAccount
          ? accs.find((a) => a.id === selectedAccount.id) || accs[0]
          : accs[0];
        setSelectedAccount(activeAcc);
        loadTransactions(activeAcc.id);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [selectedAccount]);

  const loadTransactions = async (accId: number) => {
    try {
      const txRes = await api.get<ApiResponse<Transaction[]>>(`/transfers/account/${accId}`);
      setTransactions(txRes.data.data || []);
    } catch (err) {
      console.error('Failed to load transactions:', err);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAccountChange = (acc: Account) => {
    setSelectedAccount(acc);
    loadTransactions(acc.id);
  };

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAccount) return;
    setDepositLoading(true);
    setError(null);
    try {
      await api.post<ApiResponse<Account>>('/accounts/deposit', {
        accountId: selectedAccount.id,
        amount: parseFloat(depositAmount),
      });
      setShowDepositModal(false);
      setSuccessMsg(`Simulated deposit of ₹${depositAmount} was successful.`);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDepositLoading(false);
    }
  };

  const handleAddBeneficiary = async (e: React.FormEvent) => {
    e.preventDefault();
    setBLoading(true);
    setError(null);
    try {
      await api.post<ApiResponse<Beneficiary>>('/beneficiaries', {
        nickname: bNickname,
        recipientAccountNumber: bAccountNum,
      });
      setShowBeneficiaryModal(false);
      setBNickname('');
      setBAccountNum('');
      setSuccessMsg('Beneficiary added successfully.');
      const benRes = await api.get<ApiResponse<Beneficiary[]>>('/beneficiaries');
      setBeneficiaries(benRes.data.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBLoading(false);
    }
  };

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAccount) return;

    setTxLoading(true);
    setError(null);
    setSuccessMsg(null);
    setLastTransferResult(null);

    const idempotencyKey = crypto.randomUUID();

    try {
      const res = await api.post<ApiResponse<Transaction>>(
        '/transfers',
        {
          sourceAccountId: selectedAccount.id,
          destinationAccountNumber: recipientAccountNum,
          amount: parseFloat(transferAmount),
          description: transferNote || undefined,
        },
        {
          headers: {
            'Idempotency-Key': idempotencyKey,
          },
        }
      );

      const tx = res.data.data;
      setLastTransferResult(tx);
      setTransferAmount('');
      setTransferNote('');

      if (tx.status === 'APPROVED_SIMULATED') {
        setSuccessMsg(`Transfer of ₹${tx.amount.toLocaleString()} completed instantly! Ref: ${tx.referenceId}`);
      } else if (tx.status === 'PENDING_REVIEW') {
        setSuccessMsg(`Transfer held for Fraud Analyst review (Score >= 40). Funds temporarily reserved.`);
      } else {
        setError(`Transfer blocked by Fraud Prevention Engine! Reason: ${tx.failureReason || 'Risk exceeded policy limits'}`);
      }

      await loadData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setTxLoading(false);
    }
  };

  const selectBeneficiary = (b: Beneficiary) => {
    setRecipientAccountNum(b.recipientAccountNumber);
  };

  const formatCurrency = (amt: number) => {
    return `₹${amt.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  return (
    <div className="container py-4">
      {/* Welcome & Notice Banner */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <h2 className="fw-bold mb-1">Welcome, {user?.fullName}</h2>
          <p className="text-secondary mb-0 small">
            Customer Dashboard • Real-time simulated account balances & fraud-aware transfers
          </p>
        </div>
        <div className="d-flex gap-2">
          <button
            onClick={() => setShowDepositModal(true)}
            className="btn btn-outline-success btn-sm rounded-pill px-3"
          >
            <i className="bi bi-plus-circle me-1"></i> Deposit Funds
          </button>
          <button
            onClick={() => setShowBeneficiaryModal(true)}
            className="btn btn-outline-primary btn-sm rounded-pill px-3"
          >
            <i className="bi bi-person-plus me-1"></i> Add Beneficiary
          </button>
        </div>
      </div>

      {/* Status Messages */}
      {error && (
        <div className="alert alert-danger alert-dismissible fade show small" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          <strong>Error:</strong> {error}
          <button type="button" className="btn-close" onClick={() => setError(null)}></button>
        </div>
      )}

      {successMsg && (
        <div className="alert alert-success alert-dismissible fade show small" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMsg}
          <button type="button" className="btn-close" onClick={() => setSuccessMsg(null)}></button>
        </div>
      )}

      {/* Loading state */}
      {loading && accounts.length === 0 && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading accounts...</span>
          </div>
        </div>
      )}

      {/* Account Cards Grid */}
      <div className="row g-3 mb-4">
        {accounts.map((acc) => {
          const isSelected = selectedAccount?.id === acc.id;
          return (
            <div key={acc.id} className="col-md-6 col-lg-4">
              <div
                onClick={() => handleAccountChange(acc)}
                className={`tpg-card p-4 h-100 cursor-pointer ${
                  isSelected ? 'border-primary shadow-sm' : ''
                }`}
                style={{ cursor: 'pointer', borderWidth: isSelected ? '2px' : '1px' }}
              >
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <span className="badge bg-primary text-uppercase">{acc.accountType}</span>
                  <span className="badge bg-success-subtle text-success border border-success-subtle">
                    {acc.status}
                  </span>
                </div>

                <div className="font-monospace small text-secondary mb-1">
                  {acc.maskedAccountNumber}
                </div>

                <div className="mb-3">
                  <div className="small text-secondary">Available Balance</div>
                  <div className="fs-3 fw-bold text-primary">
                    {formatCurrency(acc.availableBalance)}
                  </div>
                </div>

                <div className="d-flex justify-content-between small text-secondary pt-2 border-top">
                  <span>Ledger: {formatCurrency(acc.balance)}</span>
                  {acc.heldAmount > 0 && (
                    <span className="text-warning fw-semibold">
                      Held: {formatCurrency(acc.heldAmount)}
                    </span>
                  )}
                </div>

                <div className="mt-2 small text-secondary">
                  <span>Daily Limit: {formatCurrency(acc.dailyLimit)}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Grid: Transfer Form & Transactions Table */}
      <div className="row g-4">
        {/* Transfer Form Column */}
        <div className="col-lg-5">
          <div className="tpg-card p-4 h-100">
            <h5 className="fw-bold mb-3 d-flex align-items-center gap-2">
              <i className="bi bi-send-check text-primary"></i>
              Initiate Simulated Transfer
            </h5>

            <form onSubmit={handleTransfer}>
              <div className="mb-3">
                <label className="form-label small fw-semibold">Source Account</label>
                <select
                  className="form-select"
                  value={selectedAccount?.id || ''}
                  onChange={(e) => {
                    const acc = accounts.find((a) => a.id === Number(e.target.value));
                    if (acc) handleAccountChange(acc);
                  }}
                >
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.maskedAccountNumber} ({formatCurrency(a.availableBalance)})
                    </option>
                  ))}
                </select>
              </div>

              {/* Beneficiary Quick Select */}
              {beneficiaries.length > 0 && (
                <div className="mb-3">
                  <label className="form-label small fw-semibold">Quick-Select Beneficiary</label>
                  <div className="d-flex flex-wrap gap-1">
                    {beneficiaries.map((b) => (
                      <button
                        key={b.id}
                        type="button"
                        onClick={() => selectBeneficiary(b)}
                        className={`btn btn-sm ${
                          recipientAccountNum === b.recipientAccountNumber
                            ? 'btn-primary'
                            : 'btn-outline-secondary'
                        }`}
                      >
                        {b.nickname}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="mb-3">
                <label className="form-label small fw-semibold">Recipient Account Number</label>
                <div className="input-group">
                  <span className="input-group-text"><i className="bi bi-hash"></i></span>
                  <input
                    type="text"
                    required
                    className="form-control font-monospace"
                    placeholder="e.g. TPG100000000002"
                    value={recipientAccountNum}
                    onChange={(e) => setRecipientAccountNum(e.target.value)}
                  />
                </div>
                <small className="text-secondary d-block mt-1" style={{ fontSize: '0.75rem' }}>
                  Demo Accounts: <code>TPG100000000001</code> (John), <code>TPG100000000002</code> (Jane)
                </small>
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold">Transfer Amount (INR)</label>
                <div className="input-group">
                  <span className="input-group-text">₹</span>
                  <input
                    type="number"
                    required
                    min="1"
                    step="0.01"
                    className="form-control"
                    placeholder="0.00"
                    value={transferAmount}
                    onChange={(e) => setTransferAmount(e.target.value)}
                  />
                </div>
                {selectedAccount && parseFloat(transferAmount) > selectedAccount.availableBalance && (
                  <small className="text-danger d-block mt-1">
                    Exceeds available balance ({formatCurrency(selectedAccount.availableBalance)})!
                  </small>
                )}
                {parseFloat(transferAmount) >= 50000 && (
                  <small className="text-warning d-block mt-1">
                    ⚠️ Amounts ≥ ₹50,000 trigger Fraud Rule R1 (+30 pts) and may be held for review.
                  </small>
                )}
              </div>

              <div className="mb-4">
                <label className="form-label small fw-semibold">Purpose / Note (Optional)</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. Rent, consulting fee"
                  value={transferNote}
                  onChange={(e) => setTransferNote(e.target.value)}
                />
              </div>

              <button
                type="submit"
                disabled={txLoading || !selectedAccount || !recipientAccountNum || !transferAmount}
                className="btn btn-primary w-100 py-2 rounded-pill fw-semibold"
              >
                {txLoading ? (
                  <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                ) : (
                  <i className="bi bi-shield-check me-1"></i>
                )}
                Send Secure Transfer
              </button>
            </form>

            {/* Last Transfer Detailed Result Box */}
            {lastTransferResult && (
              <div className="mt-4 p-3 rounded border border-secondary border-opacity-25 bg-body-tertiary">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <strong className="small">Transfer Result</strong>
                  <span
                    className={`badge ${
                      lastTransferResult.status === 'APPROVED_SIMULATED'
                        ? 'bg-success'
                        : lastTransferResult.status === 'PENDING_REVIEW'
                        ? 'bg-warning text-dark'
                        : 'bg-danger'
                    }`}
                  >
                    {lastTransferResult.status}
                  </span>
                </div>
                <div className="small text-secondary mb-1">
                  <strong>Ref ID:</strong> {lastTransferResult.referenceId}
                </div>
                <div className="small text-secondary mb-1">
                  <strong>Amount:</strong> {formatCurrency(lastTransferResult.amount)}
                </div>
                {lastTransferResult.failureReason && (
                  <div className="small text-danger mt-1">
                    <strong>Reason:</strong> {lastTransferResult.failureReason}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Transactions Table Column */}
        <div className="col-lg-7">
          <div className="tpg-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="fw-bold mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-clock-history text-primary"></i>
                Transaction History
              </h5>
              <button
                onClick={() => selectedAccount && loadTransactions(selectedAccount.id)}
                className="btn btn-outline-secondary btn-sm rounded-pill"
                title="Refresh transactions"
              >
                <i className="bi bi-arrow-clockwise"></i>
              </button>
            </div>

            <div className="table-responsive">
              <table className="table table-hover align-middle small mb-0">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Reference</th>
                    <th>Type</th>
                    <th>Accounts</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-4 text-secondary">
                        No transactions recorded for this account.
                      </td>
                    </tr>
                  ) : (
                    transactions.map((tx) => (
                      <tr key={tx.id}>
                        <td>
                          {new Date(tx.createdAt).toLocaleDateString([], {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </td>
                        <td className="font-monospace text-secondary">{tx.referenceId}</td>
                        <td>
                          <span
                            className={`badge ${
                              tx.type === 'DEPOSIT'
                                ? 'bg-success-subtle text-success'
                                : tx.type === 'TRANSFER_IN'
                                ? 'bg-info-subtle text-info'
                                : 'bg-warning-subtle text-warning'
                            }`}
                          >
                            {tx.type}
                          </span>
                        </td>
                        <td className="font-monospace" style={{ fontSize: '0.75rem' }}>
                          {tx.type === 'TRANSFER_OUT' ? (
                            <span>To: {tx.destinationMaskedAccountNumber}</span>
                          ) : tx.type === 'TRANSFER_IN' ? (
                            <span>From: {tx.sourceMaskedAccountNumber}</span>
                          ) : (
                            <span>Deposit</span>
                          )}
                        </td>
                        <td className="fw-semibold">
                          {tx.type === 'TRANSFER_OUT' ? '-' : '+'}
                          {formatCurrency(tx.amount)}
                        </td>
                        <td>
                          <span
                            className={`badge ${
                              tx.status === 'APPROVED_SIMULATED'
                                ? 'bg-success'
                                : tx.status === 'PENDING_REVIEW'
                                ? 'bg-warning text-dark'
                                : 'bg-danger'
                            }`}
                          >
                            {tx.status === 'PENDING_REVIEW' ? 'HELD IN REVIEW' : tx.status}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Deposit Funds Modal */}
      {showDepositModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content tpg-card">
              <div className="modal-header border-bottom border-secondary border-opacity-25">
                <h5 className="modal-title fw-bold">Deposit Simulated Funds</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowDepositModal(false)}
                ></button>
              </div>
              <form onSubmit={handleDeposit}>
                <div className="modal-body">
                  <p className="text-secondary small">
                    Add simulated demo funds to account{' '}
                    <strong>{selectedAccount?.maskedAccountNumber}</strong>. Maximum allowable single deposit is ₹50,000.00.
                  </p>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Deposit Amount (INR)</label>
                    <div className="input-group">
                      <span className="input-group-text">₹</span>
                      <input
                        type="number"
                        required
                        min="100"
                        max="50000"
                        step="100"
                        className="form-control"
                        value={depositAmount}
                        onChange={(e) => setDepositAmount(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
                <div className="modal-footer border-top border-secondary border-opacity-25">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm rounded-pill"
                    onClick={() => setShowDepositModal(false)}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={depositLoading}
                    className="btn btn-success btn-sm rounded-pill px-4"
                  >
                    {depositLoading ? 'Depositing...' : 'Confirm Deposit'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Add Beneficiary Modal */}
      {showBeneficiaryModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content tpg-card">
              <div className="modal-header border-bottom border-secondary border-opacity-25">
                <h5 className="modal-title fw-bold">Add Beneficiary</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowBeneficiaryModal(false)}
                ></button>
              </div>
              <form onSubmit={handleAddBeneficiary}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Nickname</label>
                    <input
                      type="text"
                      required
                      className="form-control"
                      placeholder="e.g. Jane Smith (Savings)"
                      value={bNickname}
                      onChange={(e) => setBNickname(e.target.value)}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Recipient Account Number</label>
                    <input
                      type="text"
                      required
                      className="form-control font-monospace"
                      placeholder="e.g. TPG100000000002"
                      value={bAccountNum}
                      onChange={(e) => setBAccountNum(e.target.value)}
                    />
                  </div>
                </div>
                <div className="modal-footer border-top border-secondary border-opacity-25">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm rounded-pill"
                    onClick={() => setShowBeneficiaryModal(false)}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={bLoading}
                    className="btn btn-primary btn-sm rounded-pill px-4"
                  >
                    {bLoading ? 'Saving...' : 'Add Beneficiary'}
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
