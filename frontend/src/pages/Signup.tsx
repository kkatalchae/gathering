import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '@/lib/api'
import { encryptPassword } from '@/lib/crypto'

export default function Signup() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', passwordConfirm: '', nickname: '', phoneNumber: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function set(field: string) {
    return (e: React.ChangeEvent<HTMLInputElement>) => setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    setLoading(true)
    try {
      await api.post('/users/join', {
        name: form.name,
        email: form.email,
        password: encryptPassword(form.password),
        nickname: form.nickname || null,
        phoneNumber: form.phoneNumber.replace(/\D/g, '') || null,
      })
      navigate('/login')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '회원가입 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '0.75rem', border: '1.5px solid #e0e0e0',
    borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box',
  }
  const labelStyle: React.CSSProperties = { display: 'block', fontWeight: 500, marginBottom: '0.4rem', fontSize: '0.9rem' }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f7ff', padding: '2rem' }}>
      <div style={{ width: '100%', maxWidth: '480px', background: '#fff', borderRadius: '16px', padding: '2.5rem', boxShadow: '0 4px 24px rgba(0,0,0,0.08)' }}>
        <Link to="/login" style={{ color: '#667eea', textDecoration: 'none', fontSize: '0.9rem' }}>← 로그인으로</Link>
        <h1 style={{ margin: '1.5rem 0 0.5rem', fontSize: '1.8rem', fontWeight: 700 }}>회원가입</h1>
        <p style={{ color: '#888', marginBottom: '2rem', fontSize: '0.95rem' }}>새 계정을 만드세요</p>

        {error && (
          <div style={{ padding: '0.75rem 1rem', background: '#fff0f0', border: '1px solid #ffd0d0', borderRadius: '8px', color: '#c00', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={labelStyle}>이름 *</label>
              <input type="text" value={form.name} onChange={set('name')} required placeholder="이름" style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>닉네임</label>
              <input type="text" value={form.nickname} onChange={set('nickname')} placeholder="닉네임 (선택)" style={inputStyle} />
            </div>
          </div>
          <div>
            <label style={labelStyle}>이메일 *</label>
            <input type="email" value={form.email} onChange={set('email')} required placeholder="이메일" style={inputStyle} />
          </div>
          <div>
            <label style={labelStyle}>비밀번호 *</label>
            <input type="password" value={form.password} onChange={set('password')} required placeholder="6자 이상" minLength={6} style={inputStyle} />
          </div>
          <div>
            <label style={labelStyle}>비밀번호 확인 *</label>
            <input type="password" value={form.passwordConfirm} onChange={set('passwordConfirm')} required placeholder="비밀번호 재입력" style={inputStyle} />
          </div>
          <div>
            <label style={labelStyle}>전화번호</label>
            <input type="tel" value={form.phoneNumber} onChange={set('phoneNumber')} placeholder="숫자만 입력 (선택)" style={inputStyle} />
          </div>
          <button
            type="submit" disabled={loading}
            style={{ padding: '0.85rem', background: loading ? '#aaa' : '#667eea', color: '#fff', border: 'none', borderRadius: '8px', fontSize: '1rem', fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer', marginTop: '0.5rem' }}
          >
            {loading ? '가입 중...' : '가입하기'}
          </button>
        </form>
      </div>
    </div>
  )
}
