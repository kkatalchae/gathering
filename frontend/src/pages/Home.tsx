import { Link } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

export default function Home() {
  const { user, logout } = useAuth()

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1.5rem 2rem', color: '#fff' }}>
        <Link to="/" style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', textDecoration: 'none' }}>
          🌟 Gathering
        </Link>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          {user ? (
            <>
              <Link to="/my-info" style={{ color: '#fff', textDecoration: 'none' }}>{user.nickname ?? user.name}</Link>
              <button
                onClick={logout}
                style={{ padding: '0.5rem 1rem', background: 'rgba(255,255,255,0.2)', color: '#fff', border: '1px solid rgba(255,255,255,0.5)', borderRadius: '8px', cursor: 'pointer' }}
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" style={{ padding: '0.5rem 1rem', background: 'rgba(255,255,255,0.2)', color: '#fff', border: '1px solid rgba(255,255,255,0.5)', borderRadius: '8px', textDecoration: 'none' }}>
                로그인
              </Link>
              <Link to="/signup" style={{ padding: '0.5rem 1rem', background: '#fff', color: '#667eea', borderRadius: '8px', textDecoration: 'none', fontWeight: 600 }}>
                회원가입
              </Link>
            </>
          )}
        </div>
      </header>

      <main style={{ textAlign: 'center', padding: '6rem 2rem', color: '#fff' }}>
        <h1 style={{ fontSize: '3rem', fontWeight: 800, marginBottom: '1rem' }}>함께하는 즐거움</h1>
        <p style={{ fontSize: '1.2rem', opacity: 0.9, marginBottom: '3rem' }}>
          관심사가 같은 사람들과 모임을 만들고 함께 성장하세요
        </p>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link
            to="/gatherings"
            style={{ padding: '1rem 2rem', background: '#fff', color: '#667eea', borderRadius: '12px', textDecoration: 'none', fontWeight: 700, fontSize: '1.1rem' }}
          >
            모임 둘러보기
          </Link>
          {user && (
            <Link
              to="/gatherings/new"
              style={{ padding: '1rem 2rem', background: 'rgba(255,255,255,0.2)', color: '#fff', border: '2px solid #fff', borderRadius: '12px', textDecoration: 'none', fontWeight: 700, fontSize: '1.1rem' }}
            >
              새 모임 만들기
            </Link>
          )}
        </div>
      </main>
    </div>
  )
}
