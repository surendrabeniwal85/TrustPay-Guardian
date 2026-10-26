import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../../api/axiosClient';
import { useToast } from '../../context/ToastContext';
import { Beneficiary, ApiResponse } from '../../types';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { ConfirmModal } from '../../components/common/ConfirmModal';

export const BeneficiariesPage: React.FC = () => {
  const { showToast } = useToast();

  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Add Beneficiary Modal State
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [accountNumber, setAccountNumber] = useState<string>('');
  const [nickname, setNickname] = useState<string>('');
  const [verifiedName, setVerifiedName] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Delete Beneficiary Modal State
  const [deletingBeneficiary, setDeletingBeneficiary] = useState<Beneficiary | null>(null);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  const fetchBeneficiaries = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.get<ApiResponse<Beneficiary[]>>('/beneficiaries');
      setBeneficiaries(res.data?.data || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch beneficiaries';
      setError(msg);
      showToast(msg, 'danger');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchBeneficiaries();
  }, [fetchBeneficiaries]);

  const handleVerifyAccount = async () => {
    if (!accountNumber || accountNumber.trim().length < 8) {
      setVerifyError('Please enter a valid account number (e.g. TPG0000000002)');
      return;
    }

    setIsVerifying(true);
    setVerifyError(null);
    setVerifiedName(null);

    try {
      const res = await api.post<ApiResponse<{ maskedHolderName: string; maskedAccountNumber: string }>>(
        '/beneficiaries/lookup',
        { accountNumber: accountNumber.trim() }
      );
      if (res.data?.data?.maskedHolderName) {
        setVerifiedName(res.data.data.maskedHolderName);
      } else {
        setVerifyError('Account not found in bank directory');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lookup failed';
      setVerifyError(msg);
    } finally {
      setIsVerifying(false);
    }
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verifiedName) {
      showToast('Please verify the recipient account before saving', 'warning');
      return;
    }
    if (!nickname.trim()) {
      showToast('Please provide a nickname', 'warning');
      return;
    }

    setIsSubmitting(true);
    try {
      await api.post('/beneficiaries', {
        recipientAccountNumber: accountNumber.trim(),
        nickname: nickname.trim(),
      });
      showToast(`Beneficiary "${nickname}" added successfully!`, 'success');
      setShowAddModal(false);
      setAccountNumber('');
      setNickname('');
      setVerifiedName(null);
      fetchBeneficiaries();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save beneficiary';
      showToast(msg, 'danger');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deletingBeneficiary) return;

    setIsDeleting(true);
    try {
      await api.delete(`/beneficiaries/${deletingBeneficiary.id}`);
      showToast(`Beneficiary "${deletingBeneficiary.nickname}" removed`, 'info');
      setDeletingBeneficiary(null);
      fetchBeneficiaries();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete beneficiary';
      showToast(msg, 'danger');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Beneficiary Management</h1>
          <p className="text-secondary small mb-0">
            Maintain your trusted recipients directory with verified account name confirmation.
          </p>
        </div>
        <div>
          <button
            onClick={() => setShowAddModal(true)}
            className="btn btn-primary btn-sm d-flex align-items-center gap-2"
          >
            <i className="bi bi-person-plus-fill"></i>
            <span>Add Beneficiary</span>
          </button>
        </div>
      </div>

      {/* Main List */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body p-0">
          {isLoading ? (
            <div className="p-4">
              <Skeleton height="2.5rem" className="mb-2" />
              <Skeleton height="2.5rem" className="mb-2" />
              <Skeleton height="2.5rem" />
            </div>
          ) : error ? (
            <div className="p-4">
              <EmptyState
                icon="bi-exclamation-triangle"
                title="Error Loading Beneficiaries"
                description={error}
                action={
                  <button onClick={fetchBeneficiaries} className="btn btn-outline-primary btn-sm">
                    Retry
                  </button>
                }
              />
            </div>
          ) : beneficiaries.length === 0 ? (
            <div className="p-5 text-center">
              <EmptyState
                icon="bi-people"
                title="No Beneficiaries Added Yet"
                description="Save frequent transfer recipients to speed up money movement and avoid typing account numbers."
                action={
                  <button onClick={() => setShowAddModal(true)} className="btn btn-primary btn-sm">
                    Add Your First Beneficiary
                  </button>
                }
              />
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light small text-secondary">
                  <tr>
                    <th className="ps-3">Nickname</th>
                    <th>Verified Holder Name</th>
                    <th>Masked Account Number</th>
                    <th>Date Added</th>
                    <th className="pe-3 text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {beneficiaries.map((b) => (
                    <tr key={b.id}>
                      <td className="ps-3 fw-semibold">
                        <div className="d-flex align-items-center gap-2">
                          <i className="bi bi-person-circle text-primary fs-5"></i>
                          <span>{b.nickname}</span>
                        </div>
                      </td>
                      <td className="text-body">{b.recipientName}</td>
                      <td className="font-monospace small text-muted">
                        {b.recipientMaskedAccountNumber}
                      </td>
                      <td className="small text-muted">
                        {new Date(b.createdAt).toLocaleDateString([], {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </td>
                      <td className="pe-3 text-end">
                        <button
                          onClick={() => setDeletingBeneficiary(b)}
                          className="btn btn-outline-danger btn-sm p-1 px-2"
                          title="Remove beneficiary"
                        >
                          <i className="bi bi-trash"></i>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Add Beneficiary Modal */}
      {showAddModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Add Trusted Beneficiary</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowAddModal(false)}
                  disabled={isSubmitting}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleAddSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Recipient Account Number</label>
                    <div className="input-group">
                      <input
                        type="text"
                        className="form-control font-monospace"
                        placeholder="e.g. TPG0000000002"
                        value={accountNumber}
                        onChange={(e) => {
                          setAccountNumber(e.target.value.toUpperCase());
                          setVerifiedName(null);
                          setVerifyError(null);
                        }}
                        required
                      />
                      <button
                        type="button"
                        className="btn btn-outline-primary d-flex align-items-center gap-1"
                        onClick={handleVerifyAccount}
                        disabled={isVerifying || !accountNumber}
                      >
                        {isVerifying ? (
                          <span className="spinner-border spinner-border-sm" role="status"></span>
                        ) : (
                          <i className="bi bi-shield-check"></i>
                        )}
                        <span>Verify</span>
                      </button>
                    </div>

                    {verifyError && (
                      <div className="text-danger small mt-1">
                        <i className="bi bi-x-circle me-1"></i>
                        {verifyError}
                      </div>
                    )}

                    {verifiedName && (
                      <div className="p-2 rounded bg-success-subtle text-success small mt-2 d-flex align-items-center gap-2 border border-success-subtle">
                        <i className="bi bi-check-circle-fill fs-5"></i>
                        <div>
                          <div className="fw-semibold">Account Verified:</div>
                          <div>Holder: <strong>{verifiedName}</strong></div>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Beneficiary Nickname</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. John Office, Landlord, Mom"
                      value={nickname}
                      onChange={(e) => setNickname(e.target.value)}
                      maxLength={50}
                      required
                    />
                    <div className="form-text small">A personal label to quickly identify this recipient.</div>
                  </div>

                  <div className="p-2 rounded bg-body-tertiary small text-secondary">
                    <i className="bi bi-info-circle me-1 text-primary"></i>
                    To protect customer privacy, our banking API only reveals a masked recipient name during verification.
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowAddModal(false)}
                    disabled={isSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                    disabled={isSubmitting || !verifiedName}
                  >
                    {isSubmitting && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Save Beneficiary</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingBeneficiary && (
        <ConfirmModal
          isOpen={true}
          title="Remove Beneficiary"
          message={`Are you sure you want to remove "${deletingBeneficiary.nickname}" (${deletingBeneficiary.recipientMaskedAccountNumber}) from your saved beneficiaries?`}
          confirmLabel="Remove"
          cancelLabel="Keep"
          confirmVariant="danger"
          isLoading={isDeleting}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeletingBeneficiary(null)}
        />
      )}
    </div>
  );
};
