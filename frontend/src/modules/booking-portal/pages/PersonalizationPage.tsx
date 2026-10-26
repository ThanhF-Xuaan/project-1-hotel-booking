import { useCallback, useEffect, useRef, useState, type FC } from 'react'
import axios from 'axios'
import { ArrowLeft, RefreshCw, ShieldCheck, Sparkles } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Button from '../../../core/components/ui/Button'
import { getErrorStatus } from '../../../core/api/client'
import { getUserRoles, parseJwt } from '../../auth/utils/token.utils'
import { personalizationService } from '../services/personalizationService'
import type {
  PersonalizationConsentView,
  PersonalizationErrorDetail,
  PersonalizationRecommendationResponse,
  UpdatePersonalizationConsentRequest,
} from '../types/personalization.types'

interface ErrorEnvelope {
  message?: string
  result?: PersonalizationErrorDetail
}

function parseHotelId(values: string[]): number | null {
  if (values.length !== 1 || !/^[1-9]\d*$/.test(values[0])) return null
  const hotelId = Number(values[0])
  return Number.isSafeInteger(hotelId) && hotelId <= 32767 ? hotelId : null
}

function accountKey(token: string | null): string | null {
  if (!token) return null
  const claims = parseJwt(token)
  const issuer = claims?.iss
  const subject = claims?.sub
  return typeof issuer === 'string' && typeof subject === 'string' ? `${issuer}\u0000${subject}` : null
}

function getRequestErrorMessage(error: unknown): string {
  const status = getErrorStatus(error)
  const reasonCode = axios.isAxiosError<ErrorEnvelope>(error)
    ? error.response?.data?.result?.reasonCode
    : undefined

  if (status === 401) return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để tiếp tục.'
  if (status === 403) return 'Tính năng này chỉ dành cho tài khoản khách hàng.'
  if (status === 404) return 'Không tìm thấy khách sạn đang hoạt động hoặc demo U14 chưa được bật.'
  if (status === 409 && reasonCode === 'IDENTITY_LINK_REQUIRED') {
    return 'Tài khoản khách chưa được liên kết với hồ sơ demo.'
  }
  if (status === 409) return 'Consent hoặc liên kết hồ sơ vừa thay đổi. Trạng thái đã được đồng bộ lại.'
  if (status === 503) return 'Dịch vụ tạm thời chưa sẵn sàng. Bạn có thể thử lại sau.'
  if (axios.isAxiosError(error) && !error.response) return 'Không kết nối được máy chủ. Hãy kiểm tra backend rồi thử lại.'
  return 'Không thể hoàn tất yêu cầu. Hãy thử lại sau.'
}

export const PersonalizationPage: FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const hotelId = parseHotelId(searchParams.getAll('hotelId'))
  const generationRef = useRef(0)
  const recommendationRequestRef = useRef(0)
  const accountKeyRef = useRef(accountKey(localStorage.getItem('auth_token')))
  const [accountRevision, setAccountRevision] = useState(0)
  const [consent, setConsent] = useState<PersonalizationConsentView | null>(null)
  const [recommendation, setRecommendation] = useState<PersonalizationRecommendationResponse | null>(null)
  const [consentError, setConsentError] = useState<string | null>(null)
  const [recommendationError, setRecommendationError] = useState<string | null>(null)
  const [recommendationNeedsRefresh, setRecommendationNeedsRefresh] = useState(false)
  const [policyAccepted, setPolicyAccepted] = useState(false)
  const [isLoadingConsent, setIsLoadingConsent] = useState(false)
  const [isLoadingRecommendation, setIsLoadingRecommendation] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const isCustomer = getUserRoles().includes('CUSTOMER')

  const isCurrent = useCallback((generation: number) => generationRef.current === generation, [])

  const loadRecommendations = useCallback(async (targetHotelId: number, generation: number) => {
    const requestId = ++recommendationRequestRef.current
    const requestIsCurrent = () => isCurrent(generation) && recommendationRequestRef.current === requestId
    setRecommendation(null)
    setRecommendationError(null)
    setRecommendationNeedsRefresh(false)
    setIsLoadingRecommendation(true)
    try {
      const result = await personalizationService.getRecommendations(targetHotelId)
      if (!requestIsCurrent()) return
      setRecommendation(result)
      if (result.status === 'CONSENT_REQUIRED') {
        setRecommendationError('Consent không còn hiệu lực. Hãy tải lại trạng thái trước khi tiếp tục.')
        setRecommendationNeedsRefresh(true)
      }
    } catch (error: unknown) {
      if (!requestIsCurrent()) return
      setRecommendationError(getRequestErrorMessage(error))
      setRecommendationNeedsRefresh(getErrorStatus(error) === 409)
    } finally {
      if (requestIsCurrent()) setIsLoadingRecommendation(false)
    }
  }, [isCurrent])

  const loadConsent = useCallback(async (
    targetHotelId: number,
    generation: number,
    loadRecommendationsWhenGranted: boolean,
  ) => {
    recommendationRequestRef.current += 1
    setConsent(null)
    setRecommendation(null)
    setConsentError(null)
    setRecommendationError(null)
    setRecommendationNeedsRefresh(false)
    setPolicyAccepted(false)
    setIsLoadingRecommendation(false)
    setIsSaving(false)
    setIsLoadingConsent(true)
    try {
      const currentConsent = await personalizationService.getConsent(targetHotelId)
      if (!isCurrent(generation)) return
      setConsent(currentConsent)
      setIsLoadingConsent(false)
      if (loadRecommendationsWhenGranted && currentConsent.granted) {
        await loadRecommendations(targetHotelId, generation)
      }
    } catch (error: unknown) {
      if (!isCurrent(generation)) return
      setConsentError(getRequestErrorMessage(error))
    } finally {
      if (isCurrent(generation)) setIsLoadingConsent(false)
    }
  }, [isCurrent, loadRecommendations])

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.key !== 'auth_token') return
      const nextAccountKey = accountKey(event.newValue)
      if (nextAccountKey === accountKeyRef.current) return
      accountKeyRef.current = nextAccountKey
      generationRef.current += 1
      recommendationRequestRef.current += 1
      setConsent(null)
      setRecommendation(null)
      setConsentError(null)
      setRecommendationError(null)
      setRecommendationNeedsRefresh(false)
      setPolicyAccepted(false)
      setIsLoadingConsent(false)
      setIsLoadingRecommendation(false)
      setIsSaving(false)
      setAccountRevision((revision) => revision + 1)
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  useEffect(() => {
    const generation = ++generationRef.current
    recommendationRequestRef.current += 1

    if (hotelId === null || !isCustomer) return
    void loadConsent(hotelId, generation, true)

    return () => {
      if (isCurrent(generation)) generationRef.current += 1
    }
  }, [hotelId, isCustomer, accountRevision, isCurrent, loadConsent])

  const handleConsentChange = async (granted: boolean) => {
    if (hotelId === null || !consent || isSaving || (granted && !policyAccepted)) return
    const generation = generationRef.current
    recommendationRequestRef.current += 1
    setRecommendation(null)
    setRecommendationError(null)
    setRecommendationNeedsRefresh(false)
    setConsentError(null)
    setPolicyAccepted(false)
    setIsLoadingRecommendation(false)
    setIsSaving(true)

    const request: UpdatePersonalizationConsentRequest = {
      hotelId,
      granted,
      expectedRevision: consent.revision,
      expectedBindingVersion: consent.bindingVersion,
      ...(granted ? { policyVersion: consent.currentPolicyVersion } : {}),
    }

    try {
      const updatedConsent = await personalizationService.updateConsent(request)
      if (!isCurrent(generation)) return
      setConsent(updatedConsent)
      if (updatedConsent.granted) await loadRecommendations(hotelId, generation)
    } catch (error: unknown) {
      if (!isCurrent(generation)) return
      const message = getRequestErrorMessage(error)
      setConsentError(message)
      if (getErrorStatus(error) === 409) {
        setConsent(null)
        setIsLoadingConsent(true)
        try {
          const latestConsent = await personalizationService.getConsent(hotelId)
          if (isCurrent(generation)) {
            setConsent(latestConsent)
            setConsentError('Trạng thái consent đã được đồng bộ. Hãy kiểm tra lại trước khi tiếp tục.')
          }
        } catch (refreshError: unknown) {
          if (isCurrent(generation)) {
            setConsentError(`${message} Không thể tải trạng thái mới: ${getRequestErrorMessage(refreshError)}`)
          }
        } finally {
          if (isCurrent(generation)) setIsLoadingConsent(false)
        }
      }
    } finally {
      if (isCurrent(generation)) setIsSaving(false)
    }
  }

  const handleRefresh = () => {
    if (hotelId === null || !isCustomer) return
    void loadConsent(hotelId, generationRef.current, true)
  }

  if (!isCustomer) {
    return (
      <main className="min-h-screen bg-neutral-50 px-4 py-12">
        <section className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-white p-8 text-center shadow-sm">
          <h1 className="text-xl font-bold text-neutral-900">Dành cho tài khoản khách hàng</h1>
          <p className="mt-2 text-sm text-neutral-600">Vui lòng đăng nhập bằng tài khoản khách hàng để quản lý consent demo.</p>
          <Button className="mt-6" onClick={() => navigate('/portal/search')}>Về tìm phòng</Button>
        </section>
      </main>
    )
  }

  return (
    <main className="min-h-screen bg-neutral-50 px-4 py-8 sm:py-12">
      <div className="mx-auto max-w-3xl">
        <button
          type="button"
          onClick={() => navigate('/portal/search')}
          className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-neutral-600 hover:text-red-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Về tìm phòng
        </button>

        <header className="rounded-3xl bg-neutral-900 p-6 text-white sm:p-8">
          <span className="inline-flex items-center gap-2 rounded-full border border-amber-300/30 bg-amber-200/10 px-3 py-1 text-xs font-bold text-amber-200">
            <Sparkles className="h-3.5 w-3.5" /> Demo — dữ liệu giả lập
          </span>
          <h1 className="mt-4 text-2xl font-extrabold tracking-tight sm:text-3xl">Gợi ý hạng phòng cá nhân hóa</h1>
          <p className="mt-2 text-sm leading-6 text-neutral-300">
            Khách sạn #{hotelId ?? '—'} · Gợi ý không cam kết giá, khuyến mại hoặc tình trạng phòng.
          </p>
        </header>

        {hotelId === null ? (
          <section role="alert" className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-800">
            Đường dẫn thiếu hoặc có mã khách sạn không hợp lệ. Hãy quay lại tìm phòng và chọn khách sạn.
          </section>
        ) : (
          <>
            <section className="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <div className="flex items-start gap-3">
                <div className="rounded-xl bg-red-50 p-2 text-red-700"><ShieldCheck className="h-5 w-5" /></div>
                <div>
                  <h2 className="text-lg font-bold text-neutral-900">Consent phân tích lịch sử lưu trú</h2>
                  <p className="mt-1 text-sm text-neutral-600">
                    Dữ liệu chỉ được dùng để xếp hạng hạng phòng tại khách sạn đang chọn.
                  </p>
                </div>
              </div>

              {isLoadingConsent && (
                <p role="status" className="mt-6 text-sm text-neutral-600">Đang tải trạng thái consent…</p>
              )}

              {consentError && (
                <div role="alert" className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                  <p>{consentError}</p>
                  <Button variant="outline" size="sm" className="mt-3" onClick={handleRefresh}>
                    <RefreshCw className="h-4 w-4" /> Tải lại trạng thái
                  </Button>
                </div>
              )}

              {consent && !isLoadingConsent && (
                <>
                  <div className="mt-5 rounded-2xl bg-neutral-50 p-4 text-sm leading-6 text-neutral-700">
                    <p>{consent.noticeText}</p>
                    <p className="mt-2 text-xs text-neutral-500">
                      Mục đích: {consent.purpose} · Phạm vi: {consent.historyLookbackMonths} tháng · Nguồn: dữ liệu giả lập.
                    </p>
                    <p className="mt-2 text-xs">
                      <a
                        className="font-semibold text-red-700 underline underline-offset-2"
                        href={`${consent.privacyPolicyUrl}?hotelId=${hotelId}`}
                      >
                        Đọc thông tin quyền riêng tư của demo
                      </a>
                    </p>
                    {consent.status === 'GRANTED' && (
                      <p className="mt-2 font-semibold text-emerald-700">Consent đang có hiệu lực.</p>
                    )}
                    {consent.status === 'RECONSENT_REQUIRED' && (
                      <p className="mt-2 font-semibold text-amber-800">Chính sách đã đổi; cần xác nhận lại phiên bản hiện hành.</p>
                    )}
                  </div>

                  {consent.granted ? (
                    <Button
                      variant="outline"
                      className="mt-5"
                      isLoading={isSaving}
                      onClick={() => void handleConsentChange(false)}
                    >
                      Rút consent
                    </Button>
                  ) : (
                    <>
                      <label className="mt-5 flex cursor-pointer items-start gap-3 text-sm text-neutral-700">
                        <input
                          type="checkbox"
                          checked={policyAccepted}
                          onChange={(event) => setPolicyAccepted(event.target.checked)}
                          className="mt-1 h-4 w-4 accent-red-600"
                        />
                        <span>Tôi đồng ý với nội dung trên để nhận gợi ý hạng phòng không ràng buộc.</span>
                      </label>
                      <Button
                        className="mt-5"
                        isLoading={isSaving}
                        disabled={!policyAccepted || isSaving}
                        onClick={() => void handleConsentChange(true)}
                      >
                        Đồng ý và xem gợi ý
                      </Button>
                    </>
                  )}
                </>
              )}
            </section>

            {consent?.granted && (
              <section aria-labelledby="personalization-results-heading" className="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 id="personalization-results-heading" className="text-lg font-bold text-neutral-900">Hạng phòng gợi ý</h2>
                    <p className="mt-1 text-sm text-neutral-600">Gợi ý chỉ tham khảo; kiểm tra giá và tồn phòng ở bước tìm kiếm.</p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    isLoading={isLoadingRecommendation}
                    disabled={isLoadingRecommendation || isSaving}
                    onClick={() => recommendationNeedsRefresh
                      ? handleRefresh()
                      : void loadRecommendations(hotelId, generationRef.current)}
                  >
                    <RefreshCw className="h-4 w-4" /> {recommendationNeedsRefresh ? 'Đồng bộ consent' : 'Tải gợi ý'}
                  </Button>
                </div>

                {isLoadingRecommendation && <p role="status" className="mt-5 text-sm text-neutral-600">Đang tạo gợi ý…</p>}
                {recommendationError && (
                  <p role="alert" className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                    {recommendationError}
                  </p>
                )}
                {recommendation?.status === 'COLD_START_FALLBACK' && (
                  <p className="mt-5 rounded-xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                    {recommendation.reasonCode === 'NO_PUBLIC_ROOM_TYPES'
                      ? 'Khách sạn hiện chưa có hạng phòng đủ điều kiện để gợi ý.'
                      : 'Chưa có dữ liệu phù hợp để đưa ra gợi ý.'}
                  </p>
                )}
                {recommendation?.status === 'CONSENT_REQUIRED' && !recommendationError && (
                  <p className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                    Consent không còn hiệu lực. Hãy tải lại trạng thái trước khi tiếp tục.
                  </p>
                )}
                {recommendation?.status === 'PERSONALIZED_READY' && recommendation.roomTypeSuggestions.length > 0 && (
                  <div className="mt-5 grid gap-3 sm:grid-cols-2">
                    {recommendation.roomTypeSuggestions.map((suggestion) => (
                      <article key={suggestion.hotelRoomTypeId} className="rounded-2xl border border-neutral-200 p-4">
                        <p className="text-xs font-bold uppercase tracking-wide text-red-700">Hạng phòng gợi ý</p>
                        <h3 className="mt-2 font-bold text-neutral-900">{suggestion.roomTypeName}</h3>
                        <p className="mt-1 text-xs text-neutral-500">Mã {suggestion.roomTypeCode}</p>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </div>
    </main>
  )
}

export default PersonalizationPage
