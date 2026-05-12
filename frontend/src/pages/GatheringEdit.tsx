import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/contexts/AuthContext'
import GatheringForm from './GatheringForm'
import type { GatheringResponse } from '@/types'

export default function GatheringEdit() {
  const { tsid } = useParams<{ tsid: string }>()
  const { user, loading: authLoading } = useAuth()
  const navigate = useNavigate()
  const [gathering, setGathering] = useState<GatheringResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && !user) { navigate('/login'); return }
    api.get<GatheringResponse>(`/gatherings/${tsid}`)
      .then((res) => setGathering(res.data))
      .finally(() => setLoading(false))
  }, [tsid, user, authLoading])

  if (loading || authLoading) return <div style={{ textAlign: 'center', padding: '4rem' }}>불러오는 중...</div>
  if (!gathering) return <div style={{ textAlign: 'center', padding: '4rem' }}>모임을 찾을 수 없습니다.</div>

  async function handleSubmit(form: { name: string; category: string; regionTsid: string; description: string; mainImageUrl: string | null }) {
    const { data } = await api.put<GatheringResponse>(`/gatherings/${tsid}`, {
      name: form.name,
      category: form.category,
      regionTsid: form.regionTsid,
      description: form.description || null,
      mainImageUrl: gathering?.mainImageUrl ?? null,
    })
    return { tsid: data.tsid }
  }

  return <GatheringForm initialData={gathering} onSubmit={handleSubmit} submitLabel="저장하기" title="모임 수정" />
}
