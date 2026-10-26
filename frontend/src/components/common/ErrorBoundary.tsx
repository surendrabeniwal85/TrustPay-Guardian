import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
  }

  public handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="container py-5 text-center">
          <div className="card shadow-lg p-5 mx-auto border-0" style={{ maxWidth: '500px', backgroundColor: 'var(--tpg-bg-card)' }}>
            <i className="bi bi-exclamation-octagon text-danger display-3 mb-3"></i>
            <h2 className="h4 fw-bold">Something went wrong</h2>
            <p className="text-muted mb-4">
              An unexpected client error occurred. Your funds and security records remain fully intact.
            </p>
            <div>
              <button onClick={this.handleReset} className="btn btn-primary px-4">
                <i className="bi bi-arrow-clockwise me-1"></i> Reload Application
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
