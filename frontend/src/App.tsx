import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import Home from '@/pages/Home'
import Login from '@/pages/Login'
import Signup from '@/pages/Signup'
import MyInfo from '@/pages/MyInfo'
import GatheringList from '@/pages/GatheringList'
import GatheringCreate from '@/pages/GatheringCreate'
import GatheringDetail from '@/pages/GatheringDetail'
import GatheringEdit from '@/pages/GatheringEdit'

function OAuthCallback() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    const accessToken = params.get('accessToken')
    if (accessToken) {
      login(accessToken).then(() => navigate('/'))
    } else {
      navigate('/login?error=oauth_failed')
    }
  }, [])

  return <div style={{ textAlign: 'center', padding: '4rem' }}>로그인 처리 중...</div>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/my-info" element={<MyInfo />} />
      <Route path="/gatherings" element={<GatheringList />} />
      <Route path="/gatherings/new" element={<GatheringCreate />} />
      <Route path="/gatherings/:tsid" element={<GatheringDetail />} />
      <Route path="/gatherings/:tsid/edit" element={<GatheringEdit />} />
      <Route path="/oauth/callback" element={<OAuthCallback />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
