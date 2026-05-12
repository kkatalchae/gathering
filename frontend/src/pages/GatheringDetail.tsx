import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/contexts/AuthContext'
import type { GatheringDetailResponse } from '@/types'
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string

export default function GatheringDetail() {
  const { tsid } = useParams<{ tsid: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [gathering, setGathering] = useState<GatheringDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<GatheringDetailResponse>(`/gatherings/${tsid}`)
      .then((res) => setGathering(res.data))
      .finally(() => setLoading(false))
  }, [tsid])

  if (loading) return <div style={{ textAlign: 'center', padding: '4rem' }}>불러오는 중...</div>
  if (!gathering) return <div style={{ textAlign: 'center', padding: '4rem' }}>모임을 찾을 수 없습니다.</div>

  const myParticipant = user ? gathering.participants.find((p) => p.userTsid === user.tsid) : null
  const myRole = myParticipant?.role ?? null
  const isOwnerOrAdmin = myRole === 'OWNER' || myRole === 'ADMIN'

  async function handleJoin() {
    if (!user) { navigate('/login'); return }
    await api.post(`/gatherings/${tsid}/participants`)
    const res = await api.get<GatheringDetailResponse>(`/gatherings/${tsid}`)
    setGathering(res.data)
  }

  async function handleLeave() {
    await api.delete(`/gatherings/${tsid}/participants/me`)
    const res = await api.get<GatheringDetailResponse>(`/gatherings/${tsid}`)
    setGathering(res.data)
  }

  async function handleDelete() {
    if (!confirm('모임을 삭제하시겠습니까?')) return
    await api.delete(`/gatherings/${tsid}`)
    navigate('/gatherings')
  }

  const ROLE_LABELS: Record<string, string> = { OWNER: '모임장', ADMIN: '운영진', MEMBER: '일반회원' }

  return (
    <div style={{ minHeight: '100vh', background: '#f5f7ff' }}>
      <header style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Link to="/gatherings" style={{ color: '#667eea', textDecoration: 'none', fontSize: '0.9rem' }}>← 목록으로</Link>
        <Link to="/" style={{ fontSize: '1.2rem', fontWeight: 700, color: '#667eea', textDecoration: 'none' }}>🌟 Gathering</Link>
        <div>{user ? <Link to="/my-info" style={{ color: '#555', textDecoration: 'none', fontSize: '0.9rem' }}>{user.nickname ?? user.name}</Link> : <Link to="/login" style={{ color: '#667eea', textDecoration: 'none' }}>로그인</Link>}</div>
      </header>

      <div style={{ maxWidth: '800px', margin: '0 auto', padding: '2rem 1.5rem' }}>
        {/* 커버 이미지 */}
        <div style={{ height: '240px', background: 'linear-gradient(135deg, #667eea22, #764ba222)', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '6rem', overflow: 'hidden', marginBottom: '1.5rem' }}>
          {gathering.mainImageUrl
            ? <img src={`${BASE_URL}${gathering.mainImageUrl}`} alt={gathering.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            : CATEGORY_ICONS[gathering.category]}
        </div>

        {/* 정보 카드 */}
        <div style={{ background: '#fff', borderRadius: '16px', padding: '2rem', boxShadow: '0 2px 12px rgba(0,0,0,0.06)', marginBottom: '1.5rem' }}>
          <span style={{ display: 'inline-block', padding: '0.2rem 0.7rem', background: '#f0f0ff', color: '#667eea', borderRadius: '999px', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.75rem' }}>
            {CATEGORY_ICONS[gathering.category]} {CATEGORY_LABELS[gathering.category]}
          </span>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '0.5rem' }}>{gathering.name}</h1>
          <p style={{ color: '#888', marginBottom: '1rem' }}>📍 {gathering.regionName}</p>
          {gathering.description && <p style={{ color: '#555', lineHeight: 1.7 }}>{gathering.description}</p>}

          {/* 액션 버튼 */}
          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
            {!myRole && (
              <button onClick={handleJoin} style={{ padding: '0.75rem 1.5rem', background: '#667eea', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}>
                참여하기
              </button>
            )}
            {myRole && myRole !== 'OWNER' && (
              <button onClick={handleLeave} style={{ padding: '0.75rem 1.5rem', background: '#f5f5f5', color: '#555', border: '1px solid #ddd', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}>
                퇴장하기
              </button>
            )}
            {isOwnerOrAdmin && (
              <Link to={`/gatherings/${tsid}/edit`} style={{ padding: '0.75rem 1.5rem', background: '#fff', border: '1.5px solid #667eea', color: '#667eea', borderRadius: '8px', textDecoration: 'none', fontWeight: 600 }}>
                수정하기
              </Link>
            )}
            {myRole === 'OWNER' && (
              <button onClick={handleDelete} style={{ padding: '0.75rem 1.5rem', background: '#fff0f0', border: '1px solid #ffd0d0', color: '#c00', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}>
                삭제하기
              </button>
            )}
          </div>
        </div>

        {/* 참여자 목록 */}
        <div style={{ background: '#fff', borderRadius: '16px', padding: '2rem', boxShadow: '0 2px 12px rgba(0,0,0,0.06)' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.5rem' }}>참여자 ({gathering.participants.length}명)</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {gathering.participants.map((p) => (
              <div key={p.tsid} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#e0e7ff', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', flexShrink: 0 }}>
                  {p.profileImageUrl
                    ? <img src={`${BASE_URL}${p.profileImageUrl}`} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    : <span>👤</span>}
                </div>
                <span style={{ fontWeight: 500 }}>{p.nickname ?? p.name}</span>
                <span style={{ padding: '0.15rem 0.5rem', background: p.role === 'OWNER' ? '#fff0e0' : p.role === 'ADMIN' ? '#f0fff0' : '#f5f5f5', color: p.role === 'OWNER' ? '#c07000' : p.role === 'ADMIN' ? '#007a30' : '#888', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600 }}>
                  {ROLE_LABELS[p.role]}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
