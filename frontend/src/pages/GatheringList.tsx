import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/contexts/AuthContext'
import type { GatheringResponse, GatheringCategory, GatheringListResponse } from '@/types'
import { CATEGORY_LABELS, CATEGORY_ICONS } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string
const ALL_CATEGORIES = Object.keys(CATEGORY_LABELS) as GatheringCategory[]

export default function GatheringList() {
  const { user } = useAuth()
  const [items, setItems] = useState<GatheringResponse[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [selectedCategories, setSelectedCategories] = useState<GatheringCategory[]>([])
  const [loading, setLoading] = useState(false)

  const fetchGatherings = useCallback(async (reset = false) => {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      selectedCategories.forEach((c) => params.append('categories', c))
      if (!reset && cursor) params.set('cursor', cursor)
      params.set('size', '12')

      const { data } = await api.get<GatheringListResponse>(`/gatherings?${params}`)
      setItems((prev) => reset ? data.gatherings : [...prev, ...data.gatherings])
      setHasNext(data.hasNext)
      setCursor(data.nextCursor)
    } finally {
      setLoading(false)
    }
  }, [selectedCategories, cursor])

  useEffect(() => {
    setCursor(null)
    fetchGatherings(true)
  }, [selectedCategories])

  function toggleCategory(cat: GatheringCategory) {
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f5f7ff' }}>
      {/* 헤더 */}
      <header style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Link to="/" style={{ fontSize: '1.3rem', fontWeight: 700, color: '#667eea', textDecoration: 'none' }}>🌟 Gathering</Link>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          {user ? (
            <>
              {user && <Link to="/gatherings/new" style={{ padding: '0.5rem 1rem', background: '#667eea', color: '#fff', borderRadius: '8px', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 600 }}>+ 새 모임</Link>}
              <Link to="/my-info" style={{ color: '#555', textDecoration: 'none', fontSize: '0.9rem' }}>{user.nickname ?? user.name}</Link>
            </>
          ) : (
            <Link to="/login" style={{ color: '#667eea', textDecoration: 'none', fontWeight: 600 }}>로그인</Link>
          )}
        </div>
      </header>

      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '2rem 1.5rem' }}>
        <h1 style={{ fontSize: '1.6rem', fontWeight: 700, marginBottom: '1.5rem' }}>모임 목록</h1>

        {/* 카테고리 필터 */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '2rem' }}>
          <button
            onClick={() => setSelectedCategories([])}
            style={{ padding: '0.4rem 0.9rem', borderRadius: '999px', border: '1.5px solid', borderColor: selectedCategories.length === 0 ? '#667eea' : '#ddd', background: selectedCategories.length === 0 ? '#667eea' : '#fff', color: selectedCategories.length === 0 ? '#fff' : '#555', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 500 }}
          >
            전체
          </button>
          {ALL_CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => toggleCategory(cat)}
              style={{ padding: '0.4rem 0.9rem', borderRadius: '999px', border: '1.5px solid', borderColor: selectedCategories.includes(cat) ? '#667eea' : '#ddd', background: selectedCategories.includes(cat) ? '#667eea' : '#fff', color: selectedCategories.includes(cat) ? '#fff' : '#555', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 500 }}
            >
              {CATEGORY_ICONS[cat]} {CATEGORY_LABELS[cat]}
            </button>
          ))}
        </div>

        {/* 모임 그리드 */}
        {items.length === 0 && !loading ? (
          <div style={{ textAlign: 'center', padding: '4rem', color: '#aaa' }}>
            <p style={{ fontSize: '3rem', marginBottom: '1rem' }}>🔍</p>
            <p>조건에 맞는 모임이 없습니다.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem' }}>
            {items.map((g) => (
              <Link key={g.tsid} to={`/gatherings/${g.tsid}`} style={{ textDecoration: 'none' }}>
                <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', transition: 'transform 0.15s', cursor: 'pointer' }}
                  onMouseEnter={(e) => (e.currentTarget.style.transform = 'translateY(-2px)')}
                  onMouseLeave={(e) => (e.currentTarget.style.transform = 'none')}
                >
                  <div style={{ height: '160px', background: 'linear-gradient(135deg, #667eea22, #764ba222)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '4rem', overflow: 'hidden' }}>
                    {g.mainImageUrl
                      ? <img src={`${BASE_URL}${g.mainImageUrl}`} alt={g.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      : CATEGORY_ICONS[g.category]}
                  </div>
                  <div style={{ padding: '1rem' }}>
                    <span style={{ display: 'inline-block', padding: '0.2rem 0.6rem', background: '#f0f0ff', color: '#667eea', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.5rem' }}>
                      {CATEGORY_ICONS[g.category]} {CATEGORY_LABELS[g.category]}
                    </span>
                    <h3 style={{ fontWeight: 700, marginBottom: '0.3rem', color: '#1a1a1a', fontSize: '1rem' }}>{g.name}</h3>
                    <p style={{ color: '#888', fontSize: '0.85rem' }}>📍 {g.regionName}</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}

        {loading && <div style={{ textAlign: 'center', padding: '2rem', color: '#aaa' }}>불러오는 중...</div>}

        {hasNext && !loading && (
          <div style={{ textAlign: 'center', marginTop: '2rem' }}>
            <button
              onClick={() => fetchGatherings(false)}
              style={{ padding: '0.75rem 2rem', background: '#fff', border: '1.5px solid #667eea', color: '#667eea', borderRadius: '8px', cursor: 'pointer', fontWeight: 600 }}
            >
              더 보기
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
