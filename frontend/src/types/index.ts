export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface MyInfoResponse {
  tsid: string
  email: string
  name: string
  nickname: string | null
  phoneNumber: string | null
  profileImageUrl: string | null
  connectedProviders: string[]
}

export interface UserInfoResponse {
  tsid: string
  email: string
  name: string
  nickname: string | null
  profileImageUrl: string | null
}

export type GatheringCategory =
  | 'SPORTS' | 'CULTURE' | 'STUDY' | 'HOBBY' | 'FOOD'
  | 'TRAVEL' | 'NETWORKING' | 'VOLUNTEER' | 'GAME' | 'PET'
  | 'PHOTOGRAPHY' | 'MUSIC' | 'BOOK' | 'TECH' | 'INVESTMENT' | 'ETC'

export type ParticipantRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export interface Participant {
  tsid: string
  userTsid: string
  nickname: string | null
  name: string
  profileImageUrl: string | null
  role: ParticipantRole
}

export interface GatheringResponse {
  tsid: string
  name: string
  category: GatheringCategory
  regionTsid: string
  regionName: string
  description: string | null
  mainImageUrl: string | null
}

export interface GatheringDetailResponse extends GatheringResponse {
  participants: Participant[]
}

export interface GatheringListResponse {
  gatherings: GatheringResponse[]
  hasNext: boolean
  nextCursor: string | null
}

export interface RegionResponse {
  tsid: string
  name: string
  children: RegionResponse[]
}

export const CATEGORY_LABELS: Record<GatheringCategory, string> = {
  SPORTS: '스포츠', CULTURE: '문화', STUDY: '스터디', HOBBY: '취미',
  FOOD: '음식', TRAVEL: '여행', NETWORKING: '네트워킹', VOLUNTEER: '봉사',
  GAME: '게임', PET: '반려동물', PHOTOGRAPHY: '사진', MUSIC: '음악',
  BOOK: '독서', TECH: '기술', INVESTMENT: '투자', ETC: '기타',
}

export const CATEGORY_ICONS: Record<GatheringCategory, string> = {
  SPORTS: '⚽', CULTURE: '🎭', STUDY: '📚', HOBBY: '🎨',
  FOOD: '🍜', TRAVEL: '✈️', NETWORKING: '🤝', VOLUNTEER: '💚',
  GAME: '🎮', PET: '🐾', PHOTOGRAPHY: '📷', MUSIC: '🎵',
  BOOK: '📖', TECH: '💻', INVESTMENT: '📈', ETC: '✨',
}
