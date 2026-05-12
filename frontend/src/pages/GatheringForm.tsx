import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '@/lib/api'
import type { GatheringCategory, GatheringResponse, RegionResponse } from '@/types'
import { CATEGORY_LABELS, CATEGORY_ICONS } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string
const ALL_CATEGORIES = Object.keys(CATEGORY_LABELS) as GatheringCategory[]

interface FormState {
  name: string
  category: GatheringCategory | ''
  regionTsid: string
  description: string
  mainImageUrl: string | null
}

interface Props {
  initialData?: GatheringResponse
  onSubmit: (form: FormState) => Promise<{ tsid: string }>
  submitLabel: string
  title: string
}

export default function GatheringForm({ initialData, onSubmit, submitLabel, title }: Props) {
  const navigate = useNavigate()
  const fileRef = useRef<HTMLInputElement>(null)
  const [form, setForm] = useState<FormState>({
    name: initialData?.name ?? '',
    category: initialData?.category ?? '',
    regionTsid: initialData?.regionTsid ?? '',
    description: initialData?.description ?? '',
    mainImageUrl: initialData?.mainImageUrl ?? null,
  })
  const [regions, setRegions] = useState<RegionResponse[]>([])
  const [selectedDepth1, setSelectedDepth1] = useState('')
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(initialData?.mainImageUrl ? `${BASE_URL}${initialData.mainImageUrl}` : null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get<RegionResponse[]>('/regions').then((res) => {
      setRegions(res.data)
      if (initialData?.regionTsid) {
        const parent = res.data.find((r) => r.children.some((c) => c.tsid === initialData.regionTsid))
        if (parent) setSelectedDepth1(parent.tsid)
        else setSelectedDepth1(initialData.regionTsid)
      }
    })
  }, [])

  function handleImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setImageFile(file)
    setImagePreview(URL.createObjectURL(file))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.category) { setError('카테고리를 선택해주세요.'); return }
    if (!form.regionTsid) { setError('지역을 선택해주세요.'); return }
    setError('')
    setLoading(true)
    try {
      const result = await onSubmit(form)
      if (imageFile) {
        const fd = new FormData()
        fd.append('file', imageFile)
        await api.patch(`/gatherings/${result.tsid}/main-image`, fd)
      }
      navigate(`/gatherings/${result.tsid}`)
    } catch {
      setError('저장에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const depth1 = regions
  const depth2 = depth1.find((r) => r.tsid === selectedDepth1)?.children ?? []

  const inputStyle: React.CSSProperties = { width: '100%', padding: '0.75rem', border: '1.5px solid #e0e0e0', borderRadius: '8px', fontSize: '1rem', boxSizing: 'border-box' }
  const labelStyle: React.CSSProperties = { display: 'block', fontWeight: 500, marginBottom: '0.4rem', fontSize: '0.9rem' }

  return (
    <div style={{ minHeight: '100vh', background: '#f5f7ff' }}>
      <header style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '1rem 2rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <Link to="/gatherings" style={{ color: '#667eea', textDecoration: 'none', fontSize: '0.9rem' }}>← 목록으로</Link>
        <h1 style={{ fontSize: '1.2rem', fontWeight: 700 }}>{title}</h1>
      </header>

      <div style={{ maxWidth: '600px', margin: '0 auto', padding: '2rem 1.5rem' }}>
        {error && <div style={{ padding: '0.75rem 1rem', background: '#fff0f0', border: '1px solid #ffd0d0', borderRadius: '8px', color: '#c00', marginBottom: '1.5rem' }}>{error}</div>}

        <form onSubmit={handleSubmit} style={{ background: '#fff', borderRadius: '16px', padding: '2rem', boxShadow: '0 2px 12px rgba(0,0,0,0.06)', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* 이름 */}
          <div>
            <label style={labelStyle}>모임 이름 * <span style={{ color: '#aaa', fontWeight: 400 }}>({form.name.length}/25)</span></label>
            <input type="text" value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required maxLength={25} placeholder="모임 이름" style={inputStyle} />
          </div>

          {/* 카테고리 */}
          <div>
            <label style={labelStyle}>카테고리 *</label>
            <select value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value as GatheringCategory }))} required style={inputStyle}>
              <option value="">카테고리 선택</option>
              {ALL_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{CATEGORY_ICONS[cat]} {CATEGORY_LABELS[cat]}</option>
              ))}
            </select>
          </div>

          {/* 지역 */}
          <div>
            <label style={labelStyle}>지역 *</label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <select
                value={selectedDepth1}
                onChange={(e) => {
                  setSelectedDepth1(e.target.value)
                  const region = regions.find((r) => r.tsid === e.target.value)
                  setForm((p) => ({ ...p, regionTsid: region?.children.length ? '' : e.target.value }))
                }}
                required
                style={inputStyle}
              >
                <option value="">시/도 선택</option>
                {depth1.map((r) => <option key={r.tsid} value={r.tsid}>{r.name}</option>)}
              </select>
              {depth2.length > 0 && (
                <select value={form.regionTsid} onChange={(e) => setForm((p) => ({ ...p, regionTsid: e.target.value }))} required style={inputStyle}>
                  <option value="">구/군 선택</option>
                  {depth2.map((r) => <option key={r.tsid} value={r.tsid}>{r.name}</option>)}
                </select>
              )}
            </div>
          </div>

          {/* 설명 */}
          <div>
            <label style={labelStyle}>모임 소개</label>
            <textarea value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))} placeholder="모임을 소개해주세요 (선택)" rows={4} style={{ ...inputStyle, resize: 'vertical' }} />
          </div>

          {/* 이미지 */}
          <div>
            <label style={labelStyle}>대표 이미지</label>
            <div
              onClick={() => fileRef.current?.click()}
              style={{ border: '2px dashed #ddd', borderRadius: '12px', padding: '2rem', textAlign: 'center', cursor: 'pointer', background: '#fafafa', overflow: 'hidden' }}
            >
              {imagePreview
                ? <img src={imagePreview} alt="preview" style={{ maxHeight: '200px', borderRadius: '8px', objectFit: 'cover' }} />
                : <div style={{ color: '#aaa' }}><p style={{ fontSize: '2rem' }}>📸</p><p>클릭하여 이미지 선택 (최대 10MB)</p></div>}
            </div>
            <input ref={fileRef} type="file" accept="image/*" onChange={handleImageSelect} style={{ display: 'none' }} />
          </div>

          <button
            type="submit" disabled={loading}
            style={{ padding: '0.85rem', background: loading ? '#aaa' : '#667eea', color: '#fff', border: 'none', borderRadius: '8px', fontSize: '1rem', fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer' }}
          >
            {loading ? '저장 중...' : submitLabel}
          </button>
        </form>
      </div>
    </div>
  )
}
