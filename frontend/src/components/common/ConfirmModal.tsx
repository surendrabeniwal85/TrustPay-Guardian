import React, { useEffect, useRef } from 'react';

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmVariant?: 'danger' | 'primary' | 'warning' | 'success';
  isLoading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  confirmVariant = 'primary',
  isLoading = false,
  onConfirm,
  onCancel,
}) => {
  const confirmBtnRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (isOpen) {
      confirmBtnRef.current?.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onCancel]);

  if (!isOpen) return null;

  return (
    <>
      <div className="modal show d-block" tabIndex={-1} role="dialog" aria-modal="true" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)', zIndex: 1055 }}>
        <div className="modal-dialog modal-dialog-centered" role="document">
          <div className="modal-content shadow-lg border-0" style={{ backgroundColor: 'var(--tpg-bg-card)' }}>
            <div className="modal-header border-bottom" style={{ borderColor: 'var(--tpg-border-color)' }}>
              <h5 className="modal-title fw-semibold">{title}</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCancel}
                disabled={isLoading}
              ></button>
            </div>
            <div className="modal-body py-4">
              <p className="mb-0 text-body-secondary">{message}</p>
            </div>
            <div className="modal-footer border-top" style={{ borderColor: 'var(--tpg-border-color)' }}>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={onCancel}
                disabled={isLoading}
              >
                {cancelLabel}
              </button>
              <button
                ref={confirmBtnRef}
                type="button"
                className={`btn btn-${confirmVariant} d-flex align-items-center gap-2`}
                onClick={onConfirm}
                disabled={isLoading}
              >
                {isLoading && <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>}
                {confirmLabel}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
