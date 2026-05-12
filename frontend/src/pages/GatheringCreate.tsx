import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { api } from '@/lib/api'
import GatheringForm from './GatheringForm'
import type { GatheringResponse } from '@/types'

export default function GatheringCreate() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()

  if (!loading && !user) {
    navigate('/login')
    return null
  }

  async function handleSubmit(form: { name: string; category: string; regionTsid: string; description: string; mainImageUrl: string | null }) {
    const { data } = await api.post<GatheringResponse>('/gatherings', {
      name: form.name,
      category: form.category,
      regionTsid: form.regionTsid,
      description: form.description || null,
      mainImageUrl: null,
    })
    return { tsid: data.tsid }
  }

  return <GatheringForm onSubmit={handleSubmit} submitLabel="모임 만들기" title="새 모임 만들기" />
}
