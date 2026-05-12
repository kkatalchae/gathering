import { useState, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { api } from '@/lib/api'
import { encryptPassword } from '@/lib/crypto'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string

export default function MyInfo() {
  const { user, logout, login } = useAuth()
  const navigate = useNavigate()
  const fileRef = useRef<HTMLInputElement>(null)

  const [form, setForm] = useState({ name: user?.name ?? '', nickname: user?.nickname ?? '', phoneNumber: user?.phoneNumber ?? '' })
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '' })
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const [withdrawOpen, setWithdrawOpen] = useState(false)
  const [withdrawPw, setWithdrawPw] = useState('')

  if (!user) {
    return (
      <div style={{ textAlign: 'center', padding: '4rem' }}>
        <p>로그인이 필요합니다.</p>
        <Link to="/login">로그인하러 가기</Link>
      </div>
    )
  }

  async function handleProfileUpdate(e: React.FormEvent) {
    e.preventDefault()
    setMsg(''); setError('')
    try {
      await api.patch('/users/me', form)
      // refresh user
      const token = localStorage.getItem('accessToken')!
      await login(token)
      setMsg('프로필이 저장되었습니다.')
    } catch {
      setError('저장에 실패했습니다.')
    }
  }

  async function handleImageUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    const fd = new FormData()
    fd.append('file', file)
    try {
      await api.patch('/users/me/profile-image', fd)
      const token = localStorage.getItem('accessToken')!
      await login(token)
    } catch {
      setError('이미지 업로드에 실패했습니다.')
    }
  }

  async function handleImageDelete() {
    try {
      await api.delete('/users/me/profile-image')
      const token = localStorage.getItem('accessToken')!
      await login(token)
    } catch {
      setError('이미지 삭제에 실패했습니다.')
    }
  }

  async function handlePasswordChange(e: React.FormEvent) {
    e.preventDefault()
    setMsg(''); setError('')
    try {
      await api.put('/users/me/password', {
        currentPassword: pwForm.currentPassword ? encryptPassword(pwForm.currentPassword) : null,
        newPassword: encryptPassword(pwForm.newPassword),
      })
      setPwForm({ currentPassword: '', newPassword: '' })
      setMsg('비밀번호가 변경되었습니다.')
    } catch {
      setError('비밀번호 변경에 실패했습니다.')
    }
  }

  async function handleWithdraw() {
    try {
      await api.delete('/users/me', { data: { password: withdrawPw ? encryptPassword(withdrawPw) : null } })
      await logout()
      navigate('/')
    } catch {
      setError('회원 탈퇴에 실패했습니다.')
    }
  }

  const cardStyle: React.CSSProperties = { background: '#fff', borderRadius: '16px', padding: '2rem', boxShadow: '0 2px 12px rgba(0,0,0,0.06)', marginBottom: '1.5rem' }
  const inputStyle: React.CSSProperties = { width: '100%', padding: '0.75rem', border: '1.5px solid #e0e0e0', borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box' }
  const labelStyle: React.CSSProperties = { display: 'block', fontWeight: 500, marginBottom: '0.4rem', fontSize: '0.9rem' }

  return (
    <div style={{ minHeight: '100vh', background: '#f5f7ff', padding: '2rem' }}>
      <div style={{ maxWidth: '640px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <Link to="/" style={{ color: '#667eea', textDecoration: 'none' }}>← 홈으로</Link>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>내 정보</h1>
          <div />
        </div>

        {msg && <div style={{ padding: '0.75rem 1rem', background: '#f0fff4', border: '1px solid #b7ebc0', borderRadius: '8px', color: '#2a7a3b', marginBottom: '1.5rem' }}>{msg}</div>}
        {error && <div style={{ padding: '0.75rem 1rem', background: '#fff0f0', border: '1px solid #ffd0d0', borderRadius: '8px', color: '#c00', marginBottom: '1.5rem' }}>{error}</div>}

        {/* 프로필 이미지 */}
        <div style={{ ...cardStyle, display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div
            style={{ width: '80px', height: '80px', borderRadius: '50%', background: '#e0e7ff', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', cursor: 'pointer' }}
            onClick={() => fileRef.current?.click()}
          >
            {user.profileImageUrl
              ? <img src={`${BASE_URL}${user.profileImageUrl}`} alt="profile" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              : <span style={{ fontSize: '2rem' }}>👤</span>}
          </div>
          <div>
            <p style={{ fontWeight: 600, marginBottom: '0.5rem' }}>{user.nickname ?? user.name}</p>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button onClick={() => fileRef.current?.click()} style={{ padding: '0.4rem 0.8rem', background: '#667eea', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.85rem' }}>변경</button>
              {user.profileImageUrl && (
                <button onClick={handleImageDelete} style={{ padding: '0.4rem 0.8rem', background: '#fff', color: '#888', border: '1px solid #ddd', borderRadius: '6px', cursor: 'pointer', fontSize: '0.85rem' }}>삭제</button>
              )}
            </div>
          </div>
          <input ref={fileRef} type="file" accept="image/*" onChange={handleImageUpload} style={{ display: 'none' }} />
        </div>

        {/* 프로필 수정 */}
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.5rem' }}>프로필 수정</h2>
          <form onSubmit={handleProfileUpdate} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={labelStyle}>이름 *</label>
              <input type="text" value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>닉네임</label>
              <input type="text" value={form.nickname} onChange={(e) => setForm((p) => ({ ...p, nickname: e.target.value }))} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>전화번호</label>
              <input type="tel" value={form.phoneNumber} onChange={(e) => setForm((p) => ({ ...p, phoneNumber: e.target.value }))} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>이메일 (변경 불가)</label>
              <input type="email" value={user.email} disabled style={{ ...inputStyle, background: '#f5f5f5', color: '#888' }} />
            </div>
            <button type="submit" style={{ padding: '0.75rem', background: '#667eea', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 600, cursor: 'pointer' }}>저장</button>
          </form>
        </div>

        {/* 비밀번호 변경 */}
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.5rem' }}>비밀번호 변경</h2>
          <form onSubmit={handlePasswordChange} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {!user.connectedProviders.length && (
              <div>
                <label style={labelStyle}>현재 비밀번호</label>
                <input type="password" value={pwForm.currentPassword} onChange={(e) => setPwForm((p) => ({ ...p, currentPassword: e.target.value }))} style={inputStyle} />
              </div>
            )}
            <div>
              <label style={labelStyle}>새 비밀번호 *</label>
              <input type="password" value={pwForm.newPassword} onChange={(e) => setPwForm((p) => ({ ...p, newPassword: e.target.value }))} required minLength={6} style={inputStyle} />
            </div>
            <button type="submit" style={{ padding: '0.75rem', background: '#667eea', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 600, cursor: 'pointer' }}>변경</button>
          </form>
        </div>

        {/* 로그아웃 / 탈퇴 */}
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button
            onClick={async () => { await logout(); navigate('/') }}
            style={{ flex: 1, padding: '0.75rem', background: '#f0f0f0', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 500 }}
          >
            로그아웃
          </button>
          <button
            onClick={() => setWithdrawOpen(true)}
            style={{ flex: 1, padding: '0.75rem', background: '#fff0f0', border: '1px solid #ffd0d0', borderRadius: '8px', color: '#c00', cursor: 'pointer', fontWeight: 500 }}
          >
            회원 탈퇴
          </button>
        </div>

        {/* 탈퇴 모달 */}
        {withdrawOpen && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
            <div style={{ background: '#fff', borderRadius: '16px', padding: '2rem', width: '100%', maxWidth: '400px', margin: '1rem' }}>
              <h3 style={{ fontWeight: 700, marginBottom: '1rem' }}>정말 탈퇴하시겠습니까?</h3>
              <p style={{ color: '#888', marginBottom: '1.5rem', fontSize: '0.9rem' }}>이 작업은 되돌릴 수 없습니다.</p>
              {!user.connectedProviders.length && (
                <input type="password" value={withdrawPw} onChange={(e) => setWithdrawPw(e.target.value)} placeholder="비밀번호 확인" style={{ ...inputStyle, marginBottom: '1rem' }} />
              )}
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button onClick={() => setWithdrawOpen(false)} style={{ flex: 1, padding: '0.75rem', background: '#f0f0f0', border: 'none', borderRadius: '8px', cursor: 'pointer' }}>취소</button>
                <button onClick={handleWithdraw} style={{ flex: 1, padding: '0.75rem', background: '#c00', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}>탈퇴</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
