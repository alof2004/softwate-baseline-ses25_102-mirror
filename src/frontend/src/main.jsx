import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import keycloak from './auth/keycloak.js'

keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256' })
  .then(authenticated => {
    if (!authenticated) {
      window.location.reload()
      return
    }
    createRoot(document.getElementById('root')).render(
      <StrictMode>
        <App />
      </StrictMode>,
    )
  })
  .catch(() => {
    document.getElementById('root').textContent = 'Authentication service unavailable.'
  })
