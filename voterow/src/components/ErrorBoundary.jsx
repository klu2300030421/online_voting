import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error(
      'ErrorBoundary caught an error',
      this.props.componentName || 'UnknownComponent',
      error,
      errorInfo
    );
    // Log to external service in production
    if (process.env.NODE_ENV === 'production') {
      // logErrorToService(error, errorInfo);
    }
  }

  render() {
    const { hasError, error } = this.state;
    const { componentName, children } = this.props;

    if (hasError) {
      return (
        <div style={{
          border: '1px solid #ffcccb',
          background: '#fff5f5',
          color: '#b00020',
          padding: '12px',
          borderRadius: '6px',
          margin: '8px 0'
        }}>
          <strong>
            Something went wrong{componentName ? ` in ${componentName}` : ''}.
          </strong>
          {error ? (
            <pre style={{ whiteSpace: 'pre-wrap', marginTop: '8px' }}>
              {String(error?.message || error)}
            </pre>
          ) : null}
          <div style={{ marginTop: '8px' }}>
            <button 
              onClick={() => this.setState({ hasError: false, error: null })}
              style={{ marginRight: '8px', padding: '4px 8px' }}
            >
              Try Again
            </button>
            <button 
              onClick={() => window.location.reload()}
              style={{ padding: '4px 8px' }}
            >
              Reload Page
            </button>
          </div>
        </div>
      );
    }

    return children;
  }
}

export default ErrorBoundary;


