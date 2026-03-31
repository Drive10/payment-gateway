import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { ToastProviderWrapper } from './hooks/useToast'
import { ErrorBoundary } from './components/common/ErrorBoundary'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <ToastProviderWrapper>
        <App />
      </ToastProviderWrapper>
    </ErrorBoundary>
  </React.StrictMode>,
)
