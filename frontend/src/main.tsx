import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ChatPage from './pages/ChatPage'
import RequireAuth from './components/RequireAuth'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import PeoplePage from './pages/PeoplePage'
import RegisterPage from './pages/RegisterPage'
import './index.css'

const queryClient = new QueryClient()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/people"
            element={
              <RequireAuth>
                <PeoplePage />
              </RequireAuth>
            }
          />
          <Route
            path="/chat/:userId"
            element={
              <RequireAuth>
                <ChatPage />
              </RequireAuth>
            }
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
