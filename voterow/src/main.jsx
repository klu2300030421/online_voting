import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './index.css';

// Global error handler to catch base64 encoding errors
window.addEventListener('error', (event) => {
  if (event.error && event.error.message && event.error.message.includes('@smithy/util-base64')) {
    console.error('Base64 encoding error detected:', {
      message: event.error.message,
      stack: event.error.stack,
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno
    });
    console.error('This error is typically caused by:');
    console.error('1. AWS SDK being used with incorrect data types');
    console.error('2. Binary data being processed incorrectly');
    console.error('3. File upload issues with FormData');
  }
});

// Catch unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
  if (event.reason && event.reason.message && event.reason.message.includes('@smithy/util-base64')) {
    console.error('Unhandled base64 encoding promise rejection:', event.reason);
    event.preventDefault(); // Prevent the error from being logged as unhandled
  }
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);