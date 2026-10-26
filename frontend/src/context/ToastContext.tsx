import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

export type ToastType = 'success' | 'danger' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
}

interface ToastContextType {
  toasts: ToastMessage[];
  showToast: (message: string, type?: ToastType, title?: string) => void;
  removeToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

// Standalone emitter bridge for Axios client and non-React modules
let globalShowToast: ((message: string, type?: ToastType, title?: string) => void) | null = null;

export const notifyToast = (message: string, type: ToastType = 'info', title?: string) => {
  if (globalShowToast) {
    globalShowToast(message, type, title);
  } else {
    console.warn(`[Toast] ${type.toUpperCase()}: ${message}`);
  }
};

export const ToastProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((message: string, type: ToastType = 'info', title?: string) => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, title, message }]);

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
      removeToast(id);
    }, 5000);
  }, [removeToast]);

  globalShowToast = showToast;

  return (
    <ToastContext.Provider value={{ toasts, showToast, removeToast }}>
      {children}
      {/* Toast Container rendered fixed top-right */}
      <div className="toast-container position-fixed top-0 end-0 p-3" style={{ zIndex: 1100 }}>
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast show border-0 text-white bg-${toast.type} mb-2 shadow-lg animate-fade-in`}
            role="alert"
            aria-live="assertive"
            aria-atomic="true"
          >
            <div className="d-flex align-items-center">
              <div className="toast-body">
                {toast.title && <strong className="d-block mb-1">{toast.title}</strong>}
                {toast.message}
              </div>
              <button
                type="button"
                className="btn-close btn-close-white me-2 m-auto"
                onClick={() => removeToast(toast.id)}
                aria-label="Close"
              />
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = (): ToastContextType => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};
