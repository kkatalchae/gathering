import { createContext, useContext, useEffect, useState } from 'react'
import type { MyInfoResponse } from '@/types'
import { api } from '@/lib/api'

interface AuthContextValue {
  user: MyInfoResponse | null
  loading: boolean
  login: (accessToken: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<MyInfoResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setLoading(false)
      return
    }
    api.get<MyInfoResponse>('/users/me')
      .then((res) => setUser(res.data))
      .catch(() => localStorage.removeItem('accessToken'))
      .finally(() => setLoading(false))
  }, [])

  async function login(accessToken: string) {
    localStorage.setItem('accessToken', accessToken)
    const res = await api.get<MyInfoResponse>('/users/me')
    setUser(res.data)
  }

  async function logout() {
    await api.post('/logout').catch(() => {})
    localStorage.removeItem('accessToken')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
