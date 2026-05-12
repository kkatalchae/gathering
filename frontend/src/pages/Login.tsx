import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { encryptPassword } from '@/lib/crypto'
import { useAuth } from '@/contexts/AuthContext'
import type { LoginResponse } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string

export default function Login() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(params.get('error') ? '로그인에 실패했습니다.' : '')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post<LoginResponse>('/login', {
        email,
        password: encryptPassword(password),
      })
      await login(data.accessToken)
      navigate('/')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f7ff' }}>
      <div style={{ width: '100%', maxWidth: '400px', background: '#fff', borderRadius: '16px', padding: '2.5rem', boxShadow: '0 4px 24px rgba(0,0,0,0.08)' }}>
        <Link to="/" style={{ color: '#667eea', textDecoration: 'none', fontSize: '0.9rem' }}>← 홈으로</Link>
        <h1 style={{ margin: '1.5rem 0 0.5rem', fontSize: '1.8rem', fontWeight: 700 }}>로그인</h1>
        <p style={{ color: '#888', marginBottom: '2rem', fontSize: '0.95rem' }}>계정에 로그인하세요</p>

        {error && (
          <div style={{ padding: '0.75rem 1rem', background: '#fff0f0', border: '1px solid #ffd0d0', borderRadius: '8px', color: '#c00', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontWeight: 500, marginBottom: '0.4rem', fontSize: '0.9rem' }}>이메일</label>
            <input
              type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              required placeholder="이메일 입력"
              style={{ width: '100%', padding: '0.75rem', border: '1.5px solid #e0e0e0', borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontWeight: 500, marginBottom: '0.4rem', fontSize: '0.9rem' }}>비밀번호</label>
            <input
              type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              required placeholder="비밀번호 입력" minLength={6}
              style={{ width: '100%', padding: '0.75rem', border: '1.5px solid #e0e0e0', borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box' }}
            />
          </div>
          <button
            type="submit" disabled={loading}
            style={{ padding: '0.85rem', background: loading ? '#aaa' : '#667eea', color: '#fff', border: 'none', borderRadius: '8px', fontSize: '1rem', fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer', marginTop: '0.5rem' }}
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div style={{ margin: '1.5rem 0', textAlign: 'center', color: '#ccc', position: 'relative' }}>
          <hr style={{ border: 'none', borderTop: '1px solid #eee' }} />
          <span style={{ position: 'absolute', top: '-10px', left: '50%', transform: 'translateX(-50%)', background: '#fff', padding: '0 0.75rem', color: '#aaa', fontSize: '0.85rem' }}>또는</span>
        </div>

        <a
          href={`${BASE_URL}/oauth2/authorization/google`}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.85rem', border: '1.5px solid #e0e0e0', borderRadius: '8px', textDecoration: 'none', color: '#333', fontWeight: 500 }}
        >
          🔑 Google로 로그인
        </a>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', color: '#888', fontSize: '0.9rem' }}>
          계정이 없으신가요?{' '}
          <Link to="/signup" style={{ color: '#667eea', fontWeight: 600, textDecoration: 'none' }}>회원가입</Link>
        </p>
      </div>
    </div>
  )
}
