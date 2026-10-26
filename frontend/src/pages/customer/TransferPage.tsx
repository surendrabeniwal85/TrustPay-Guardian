import React, { useState, useEffect, useCallback, useId } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/axiosClient';
import { useToast } from '../../context/ToastContext';
import { Account, Beneficiary, Transaction, ApiResponse } from '../../types';
import { Skeleton } from '../../components/common/Skeleton';

interface TransferResult {
  referenceId: string;
  status: 'APPROVED_SIMULATED' | 'PENDING_REVIEW' | 'BLOCKED';
  amount: number;
  failureReason?: string;
}

export const TransferPage: React.FC = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const fallbackId = useId();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [isLoadingInitial, setIsLoadingInitial] = useState<boolean>(true);

  // Form State
  const [sourceAccountId, setSourceAccountId] = useState<number | ''>('');
  const [recipientMode, setRecipientMode] = useState<'BENEFICIARY' | 'MANUAL'>('BENEFICIARY');
  const [selectedBeneficiaryId, setSelectedBeneficiaryId] = useState<number | ''>('');
  const [manualAccountNumber, setManualAccountNumber] = useState<string>('');
  const [manualRecipientName, setManualRecipientName] = useState<string | null>(null);
  const [isLookingUp, setIsLookingUp] = useState<boolean>(false);
  const [lookupError, setLookupError] = useState<string | null>(null);

  const [amount, setAmount] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Confirmation Modal & Idempotency
  const [showConfirmModal, setShowConfirmModal] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [idempotencyKey, setIdempotencyKey] = useState<string>(() => {
    return typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'key-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
  });

  // Result Modal
  const [result, setResult] = useState<TransferResult | null>(null);

  const loadInitialData = useCallback(async () => {
    setIsLoadingInitial(true);
    try {
      const [accRes, benRes] = await Promise.all([
        api.get<ApiResponse<Account[]>>('/accounts'),
        api.get<ApiResponse<Beneficiary[]>>('/beneficiaries'),
      ]);

      const accList = accRes.data?.data || [];
      const benList = benRes.data?.data || [];

      setAccounts(accList);
      setBeneficiaries(benList);

      if (accList.length > 0) {
        setSourceAccountId(accList[0].id);
      }
      if (benList.length > 0) {
        setSelectedBeneficiaryId(benList[0].id);
      } else {
        setRecipientMode('MANUAL');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load transfer dependencies';
      showToast(msg, 'danger');
    } finally {
      setIsLoadingInitial(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  // Selected account details
  const selectedAccount = accounts.find((a) => a.id === Number(sourceAccountId));

  // Handle manual recipient lookup
  const handleLookupRecipient = async () => {
    if (!manualAccountNumber || manualAccountNumber.trim().length < 8) {
      setLookupError('Enter a valid account number (e.g. TPG0000000002)');
      return;
    }

    setIsLookingUp(true);
    setLookupError(null);
    setManualRecipientName(null);

    try {
      const res = await api.post<ApiResponse<{ maskedHolderName: string; maskedAccountNumber: string }>>(
        '/beneficiaries/lookup',
        { accountNumber: manualAccountNumber.trim() }
      );
      if (res.data?.data?.maskedHolderName) {
        setManualRecipientName(res.data.data.maskedHolderName);
      } else {
        setLookupError('Account not found in simulated directory');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lookup failed';
      setLookupError(msg);
    } finally {
      setIsLookingUp(false);
    }
  };

  const handleReviewStep = (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedAccount) {
      showToast('Please select a source account', 'warning');
      return;
    }

    if (selectedAccount.status !== 'ACTIVE') {
      showToast(`Account is ${selectedAccount.status}. Only ACTIVE accounts can make transfers.`, 'danger');
      return;
    }

    const amountNum = parseFloat(amount);
    if (isNaN(amountNum) || amountNum <= 0) {
      showToast('Enter a valid transfer amount', 'warning');
      return;
    }

    if (amountNum > selectedAccount.availableBalance) {
      showToast(
        `Insufficient available balance. Available: ₹${selectedAccount.availableBalance.toLocaleString('en-IN')}`,
        'warning'
      );
      return;
    }

    if (recipientMode === 'MANUAL' && !manualRecipientName) {
      showToast('Please verify the recipient account before proceeding', 'warning');
      return;
    }

    setShowConfirmModal(true);
  };

  const getRecipientAccountNumber = (): string => {
    if (recipientMode === 'BENEFICIARY') {
      const ben = beneficiaries.find((b) => b.id === Number(selectedBeneficiaryId));
      return ben ? ben.recipientAccountNumber : '';
    }
    return manualAccountNumber.trim();
  };

  const getRecipientDisplayName = (): string => {
    if (recipientMode === 'BENEFICIARY') {
      const ben = beneficiaries.find((b) => b.id === Number(selectedBeneficiaryId));
      return ben ? `${ben.nickname} (${ben.recipientName})` : '';
    }
    return manualRecipientName || manualAccountNumber;
  };

  const handleConfirmTransfer = async () => {
    if (isSubmitting) return; // Prevent double-clicking

    setIsSubmitting(true);
    try {
      const payload = {
        sourceAccountId: Number(sourceAccountId),
        recipientAccountNumber: getRecipientAccountNumber(),
        amount: parseFloat(amount),
        notes: notes.trim() || undefined,
      };

      const res = await api.post<ApiResponse<Transaction>>(
        '/transactions/transfer',
        payload,
        {
          headers: {
            'Idempotency-Key': idempotencyKey,
          },
        }
      );

      const txn = res.data?.data;
      if (txn) {
        setResult({
          referenceId: txn.referenceId,
          status: txn.status as 'APPROVED_SIMULATED' | 'PENDING_REVIEW' | 'BLOCKED',
          amount: txn.amount,
          failureReason: txn.failureReason,
        });
        setShowConfirmModal(false);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Transfer request failed';
      showToast(msg, 'danger');
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetFormForNextTransfer = () => {
    setResult(null);
    setAmount('');
    setNotes('');
    setManualAccountNumber('');
    setManualRecipientName(null);
    // Generate fresh idempotency key for next transaction
    const newKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'key-' + Date.now() + '-' + fallbackId;
    setIdempotencyKey(newKey);
    loadInitialData();
  };

  if (isLoadingInitial) {
    return (
      <div className="container py-4" style={{ maxWidth: '720px' }}>
        <Skeleton height="3rem" className="mb-4" />
        <Skeleton height="15rem" className="mb-3" />
        <Skeleton height="10rem" />
      </div>
    );
  }

  return (
    <div className="container py-4" style={{ maxWidth: '760px' }}>
      {/* Page Title */}
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1">Send Money (Instant Transfer)</h1>
        <p className="text-secondary small mb-0">
          Transfer simulated funds to verified accounts with real-time heuristic fraud screening.
        </p>
      </div>

      {/* Main Transfer Form Card */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body p-4">
          <form onSubmit={handleReviewStep}>
            {/* Step 1: Select Source Account */}
            <div className="mb-4">
              <label className="form-label small fw-semibold text-secondary">1. Choose Source Account</label>
              <select
                className="form-select form-select-lg"
                value={sourceAccountId}
                onChange={(e) => setSourceAccountId(Number(e.target.value))}
                required
              >
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountType} &bull; {acc.maskedAccountNumber} &mdash; Available: ₹{' '}
                    {acc.availableBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </option>
                ))}
              </select>

              {selectedAccount && (
                <div className="d-flex justify-content-between align-items-center mt-2 px-1 text-secondary small">
                  <span>
                    Total Ledger: ₹{' '}
                    {selectedAccount.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </span>
                  <span>
                    Daily Limit Remaining: ₹{' '}
                    {selectedAccount.dailyLimit.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </span>
                </div>
              )}
            </div>

            {/* Step 2: Recipient Selection */}
            <div className="mb-4">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <label className="form-label small fw-semibold text-secondary mb-0">2. Recipient</label>
                <div className="btn-group btn-group-sm">
                  <button
                    type="button"
                    className={`btn ${
                      recipientMode === 'BENEFICIARY' ? 'btn-primary' : 'btn-outline-secondary'
                    }`}
                    onClick={() => setRecipientMode('BENEFICIARY')}
                  >
                    Saved Beneficiaries
                  </button>
                  <button
                    type="button"
                    className={`btn ${
                      recipientMode === 'MANUAL' ? 'btn-primary' : 'btn-outline-secondary'
                    }`}
                    onClick={() => setRecipientMode('MANUAL')}
                  >
                    Enter Account Number
                  </button>
                </div>
              </div>

              {recipientMode === 'BENEFICIARY' ? (
                <div>
                  {beneficiaries.length === 0 ? (
                    <div className="p-3 border rounded bg-body-tertiary text-center small text-secondary">
                      No saved beneficiaries found. Please switch to "Enter Account Number" or add one first.
                    </div>
                  ) : (
                    <select
                      className="form-select"
                      value={selectedBeneficiaryId}
                      onChange={(e) => setSelectedBeneficiaryId(Number(e.target.value))}
                      required
                    >
                      {beneficiaries.map((b) => (
                        <option key={b.id} value={b.id}>
                          {b.nickname} &mdash; {b.recipientName} ({b.recipientMaskedAccountNumber})
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              ) : (
                <div>
                  <div className="input-group">
                    <input
                      type="text"
                      className="form-control font-monospace"
                      placeholder="e.g. TPG0000000002"
                      value={manualAccountNumber}
                      onChange={(e) => {
                        setManualAccountNumber(e.target.value.toUpperCase());
                        setManualRecipientName(null);
                        setLookupError(null);
                      }}
                      required
                    />
                    <button
                      type="button"
                      className="btn btn-outline-primary d-flex align-items-center gap-1"
                      onClick={handleLookupRecipient}
                      disabled={isLookingUp || !manualAccountNumber}
                    >
                      {isLookingUp ? (
                        <span className="spinner-border spinner-border-sm" role="status"></span>
                      ) : (
                        <i className="bi bi-search"></i>
                      )}
                      <span>Verify</span>
                    </button>
                  </div>

                  {lookupError && (
                    <div className="text-danger small mt-1">
                      <i className="bi bi-x-circle me-1"></i>
                      {lookupError}
                    </div>
                  )}

                  {manualRecipientName && (
                    <div className="text-success small mt-1 d-flex align-items-center gap-1">
                      <i className="bi bi-check-circle-fill"></i>
                      <span>Verified Holder: <strong>{manualRecipientName}</strong></span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Step 3: Amount & Notes */}
            <div className="row g-3 mb-4">
              <div className="col-md-6">
                <label className="form-label small fw-semibold text-secondary">3. Amount (₹)</label>
                <div className="input-group input-group-lg">
                  <span className="input-group-text fw-bold">₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    max={selectedAccount?.availableBalance || 200000}
                    className="form-control fw-bold"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    required
                  />
                </div>
                <div className="form-text small">Min: ₹1.00 &bull; Max single transfer: ₹2,00,000.00</div>
              </div>

              <div className="col-md-6">
                <label className="form-label small fw-semibold text-secondary">Note / Description (Optional)</label>
                <input
                  type="text"
                  maxLength={100}
                  className="form-control"
                  placeholder="e.g. Rent, consulting fee, groceries"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
                <div className="form-text small">Recorded on your transaction statement</div>
              </div>
            </div>

            {/* Fraud Rule Notice */}
            <div className="p-3 rounded bg-body-tertiary border mb-4 small text-secondary">
              <div className="d-flex align-items-center gap-2 mb-1 fw-semibold text-body">
                <i className="bi bi-shield-check text-primary"></i>
                <span>Explainable Fraud Screening Active</span>
              </div>
              Transfers evaluated with risk scores:
              <ul className="mb-0 ps-3 mt-1" style={{ fontSize: '0.8rem' }}>
                <li>Score &lt; 40: <strong>Instant Execution</strong> (Approved simulated)</li>
                <li>Score 40–69: <strong>Held for Analyst Review</strong> (Funds reserved from available balance)</li>
                <li>Score &ge; 70: <strong>Blocked</strong> (No funds moved)</li>
              </ul>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="btn btn-primary btn-lg w-100 py-3 fw-bold d-flex align-items-center justify-content-center gap-2"
              disabled={isSubmitting}
            >
              <span>Review &amp; Confirm Transfer</span>
              <i className="bi bi-arrow-right"></i>
            </button>
          </form>
        </div>
      </div>

      {/* Review & Confirm Modal */}
      {showConfirmModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Confirm Transfer Details</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowConfirmModal(false)}
                  disabled={isSubmitting}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body">
                <div className="text-center py-3 mb-3 border-bottom">
                  <div className="text-secondary small mb-1">Transferring</div>
                  <div className="display-6 fw-bold text-primary">
                    ₹ {parseFloat(amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                </div>

                <ul className="list-group list-group-flush small mb-3">
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Source Account:</span>
                    <span className="fw-semibold font-monospace">{selectedAccount?.maskedAccountNumber}</span>
                  </li>
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Recipient Account:</span>
                    <span className="fw-semibold font-monospace">{getRecipientAccountNumber()}</span>
                  </li>
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Recipient Name:</span>
                    <span className="fw-semibold">{getRecipientDisplayName()}</span>
                  </li>
                  {notes && (
                    <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                      <span className="text-secondary">Note:</span>
                      <span className="text-body">{notes}</span>
                    </li>
                  )}
                  <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                    <span className="text-secondary">Idempotency Guard:</span>
                    <span className="font-monospace text-muted" style={{ fontSize: '0.7rem' }}>
                      {idempotencyKey.substring(0, 18)}...
                    </span>
                  </li>
                </ul>

                <div className="alert alert-info py-2 px-3 small mb-0">
                  <i className="bi bi-lock me-1"></i>
                  Ascending-order account row locking and idempotent deduplication will be applied server-side.
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowConfirmModal(false)}
                  disabled={isSubmitting}
                >
                  Edit Transfer
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                  onClick={handleConfirmTransfer}
                  disabled={isSubmitting}
                >
                  {isSubmitting && <span className="spinner-border spinner-border-sm" role="status"></span>}
                  <span>{isSubmitting ? 'Authorizing...' : 'Authorize & Send'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Result Modal: APPROVED, PENDING_REVIEW, or BLOCKED */}
      {result && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.7)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow text-center p-3" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-body py-4">
                {result.status === 'APPROVED_SIMULATED' && (
                  <div>
                    <div className="rounded-circle bg-success-subtle text-success d-inline-flex p-3 mb-3">
                      <i className="bi bi-check-circle-fill fs-1"></i>
                    </div>
                    <h3 className="h4 fw-bold text-success mb-2">Transfer Completed Successfully</h3>
                    <p className="text-secondary small mb-4">
                      Simulated funds have been deducted from your account and credited to recipient.
                    </p>
                  </div>
                )}

                {result.status === 'PENDING_REVIEW' && (
                  <div>
                    <div className="rounded-circle bg-warning-subtle text-warning-emphasis d-inline-flex p-3 mb-3">
                      <i className="bi bi-shield-exclamation fs-1"></i>
                    </div>
                    <h3 className="h4 fw-bold text-warning-emphasis mb-2">Transfer Held for Review</h3>
                    <p className="text-secondary small mb-3">
                      Your transfer has been temporarily held for safety verification by our fraud analysts.
                    </p>
                    <div className="p-3 rounded bg-warning-subtle text-start text-warning-emphasis small mb-4 border border-warning-subtle">
                      <strong>Why was this held?</strong>
                      <div className="mt-1">
                        Our real-time screening rules identified elevated risk characteristics (e.g. transfer to a newly added beneficiary or high volume).
                      </div>
                      <div className="mt-2 text-muted" style={{ fontSize: '0.8rem' }}>
                        &bull; Funds are reserved from your available balance.
                        <br />
                        &bull; No money moves until the analyst releases or confirms the block.
                      </div>
                    </div>
                  </div>
                )}

                {result.status === 'BLOCKED' && (
                  <div>
                    <div className="rounded-circle bg-danger-subtle text-danger d-inline-flex p-3 mb-3">
                      <i className="bi bi-shield-x fs-1"></i>
                    </div>
                    <h3 className="h4 fw-bold text-danger mb-2">Transfer Blocked by Policy</h3>
                    <p className="text-secondary small mb-3">
                      This transaction was halted by our security engine to protect your account.
                    </p>
                    {result.failureReason && (
                      <div className="alert alert-danger py-2 small mb-4">
                        {result.failureReason}
                      </div>
                    )}
                    <div className="text-secondary small mb-4">
                      No funds were debited from your account. An alert has been logged for internal security audit.
                    </div>
                  </div>
                )}

                {/* Reference Details */}
                <div className="p-3 rounded bg-body-tertiary text-start mb-4">
                  <div className="d-flex justify-content-between small text-secondary mb-1">
                    <span>Transaction Reference:</span>
                    <span className="font-monospace fw-bold text-body">{result.referenceId}</span>
                  </div>
                  <div className="d-flex justify-content-between small text-secondary">
                    <span>Amount:</span>
                    <span className="fw-semibold">
                      ₹ {result.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="d-flex justify-content-center gap-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={resetFormForNextTransfer}
                  >
                    Make Another Transfer
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={() => navigate('/app/history')}
                  >
                    View in History
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
