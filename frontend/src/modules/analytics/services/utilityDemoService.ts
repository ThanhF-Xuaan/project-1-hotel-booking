import { z } from 'zod'
import apiClient from '../../../core/api/client'

export const UTILITY_SCENARIOS = [
  ['ELECTRIC_FLAT', 'Điện: ổn định', 'KWH', 1], ['WATER_FLAT', 'Nước: ổn định', 'M3', 2],
  ['ELECTRIC_SPIKES', 'Điện: tăng và giảm đột biến', 'KWH', 1], ['WATER_SPIKES', 'Nước: tăng và giảm đột biến', 'M3', 2],
  ['ZERO_BOUNDARY', 'Điện: ngưỡng tiêu thụ gần 0', 'KWH', 1], ['MAD_NOISE', 'Điện: dao động nền', 'KWH', 1],
  ['GAP', 'Nước: thiếu ngày ghi chỉ số', 'M3', 2], ['RESET', 'Điện: đặt lại đồng hồ', 'KWH', 1],
  ['REPLACEMENT', 'Nước: thay đồng hồ', 'M3', 2], ['NEGATIVE_DROP', 'Điện: chỉ số giảm bất thường', 'KWH', 1],
  ['LOW_COVERAGE', 'Nước: chưa đủ dữ liệu', 'M3', 2], ['SLOW_VARIATION', 'Điện: thay đổi chậm', 'KWH', 1],
] as const
export type UtilityScenario = typeof UTILITY_SCENARIOS[number][0]
const ids = ['ELECTRIC_FLAT', 'WATER_FLAT', 'ELECTRIC_SPIKES', 'WATER_SPIKES', 'ZERO_BOUNDARY', 'MAD_NOISE', 'GAP', 'RESET', 'REPLACEMENT', 'NEGATIVE_DROP', 'LOW_COVERAGE', 'SLOW_VARIATION'] as const
const scenario = z.enum(ids)
const value = z.number().min(0).max(1_000_000_000)
const reason = z.enum(['ROBUST_DEVIATION', 'MISSING_READING', 'RESET', 'REPLACEMENT', 'NEGATIVE_READING', 'MISSING_PREDECESSOR', 'INVALID_PREDECESSOR', 'NEGATIVE_DELTA', 'FEWER_THAN_28_VALID_PRIOR_POINTS'])
const point = z.object({
  date: z.string(), status: z.enum(['NORMAL', 'NEEDS_REVIEW', 'EXCLUDED', 'INSUFFICIENT_COVERAGE']),
  reasonCode: reason.nullable(), usage: value.nullable(), referenceCount: z.number().int().min(0).max(41),
  median: value.nullable(), mad: value.nullable(), threshold: z.number().min(0.001).max(10_000_000_000).nullable(),
  label: z.literal('Cần kiểm tra').nullable(),
}).refine(row => {
  if (row.status === 'EXCLUDED') return row.usage === null && row.median === null && row.mad === null && row.threshold === null && row.label === null
    && row.reasonCode !== null && !['ROBUST_DEVIATION', 'FEWER_THAN_28_VALID_PRIOR_POINTS'].includes(row.reasonCode)
  if (row.status === 'INSUFFICIENT_COVERAGE') return row.usage !== null && row.referenceCount < 28 && row.median === null && row.mad === null
    && row.threshold === null && row.label === null && row.reasonCode === 'FEWER_THAN_28_VALID_PRIOR_POINTS'
  return row.usage !== null && row.referenceCount >= 28 && row.median !== null && row.mad !== null && row.threshold !== null
    && (row.status === 'NORMAL' ? row.reasonCode === null && row.label === null : row.reasonCode === 'ROBUST_DEVIATION' && row.label === 'Cần kiểm tra')
})
const count = z.number().int().min(0).max(42)
const reference = z.literal('2026-10-30T17:00:00Z')
const schema = z.object({
  scenario, dataMode: z.literal('SYNTHETIC'), businessData: z.literal(false), referenceInstant: reference,
  scenarios: z.array(z.object({ id: scenario, label: z.string().min(1).max(100), unit: z.enum(['KWH', 'M3']) })).length(12),
  analysis: z.object({
    status: z.enum(['ANALYZED', 'INSUFFICIENT_COVERAGE']), reasonCode: z.literal('INSUFFICIENT_VALID_REFERENCE').nullable(),
    hotelId: z.number().int(), meterId: z.number().int(), unit: z.enum(['KWH', 'M3']),
    dataMode: z.literal('SYNTHETIC'), modelVersion: z.literal('u22-causal-median-mad-v1'), asOf: reference,
    fromInclusive: z.literal('2026-09-19'), toInclusive: z.literal('2026-10-30'), points: z.array(point).length(42),
  }),
  counts: z.object({ valid: count, evaluated: count, needsReview: count, excluded: count, insufficient: count }),
})
export type UtilityDemo = z.infer<typeof schema>
export type UtilityPoint = UtilityDemo['analysis']['points'][number]

export const utilityDemoService = {
  async get(selected: UtilityScenario, signal: AbortSignal): Promise<UtilityDemo> {
    const response = await apiClient.get<unknown>('/api/v1/ai/demo/utility-anomalies', { params: { scenario: selected }, signal })
    if (response.code !== 200) throw new Error('Invalid demo envelope')
    const result = schema.parse(response.result)
    const index = ids.indexOf(selected), meta = UTILITY_SCENARIOS[index]
    const { analysis, counts } = result
    const points = analysis.points
    const evaluated = points.filter(p => p.status === 'NORMAL' || p.status === 'NEEDS_REVIEW').length
    let references = 0
    const causalMetadataValid = points.every(p => {
      if (p.status === 'EXCLUDED' && p.reasonCode !== 'MISSING_READING' && p.reasonCode !== 'MISSING_PREDECESSOR') references = 0
      if (p.referenceCount !== references) return false
      if (p.status !== 'EXCLUDED') references++
      return true
    })
    if (result.scenario !== selected || analysis.unit !== meta[2] || analysis.hotelId !== meta[3] || analysis.meterId !== index + 1
      || result.scenarios.some((s, i) => s.id !== UTILITY_SCENARIOS[i][0] || s.unit !== UTILITY_SCENARIOS[i][2])
      || points.some((p, i) => p.date !== new Date(Date.UTC(2026, 8, 19 + i)).toISOString().slice(0, 10))
      || !causalMetadataValid || counts.valid !== points.filter(p => p.usage !== null).length || counts.evaluated !== evaluated
      || counts.needsReview !== points.filter(p => p.status === 'NEEDS_REVIEW').length
      || counts.excluded !== points.filter(p => p.status === 'EXCLUDED').length
      || counts.insufficient !== points.filter(p => p.status === 'INSUFFICIENT_COVERAGE').length
      || (evaluated > 0 ? analysis.status !== 'ANALYZED' || analysis.reasonCode !== null
        : analysis.status !== 'INSUFFICIENT_COVERAGE' || analysis.reasonCode !== 'INSUFFICIENT_VALID_REFERENCE')) {
      throw new Error('Invalid synthetic utility replay')
    }
    return result
  },
}

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 8 })
export const utilityNumber = (value: number | null) => value === null ? '—' : number.format(value)
export const utilityDate = (value: string) => value.split('-').reverse().join('/')
export const utilityUnit = (unit: UtilityDemo['analysis']['unit']) => unit === 'KWH' ? 'kWh' : 'm³'
export const utilityStatus: Record<UtilityPoint['status'], string> = {
  NORMAL: 'Trong ngưỡng', NEEDS_REVIEW: 'Cần kiểm tra', EXCLUDED: 'Bị loại', INSUFFICIENT_COVERAGE: 'Chưa đủ tham chiếu',
}
export const utilityReason: Record<NonNullable<UtilityPoint['reasonCode']>, string> = {
  ROBUST_DEVIATION: 'Lệch khỏi ngưỡng tham chiếu', MISSING_READING: 'Thiếu chỉ số', RESET: 'Đặt lại đồng hồ',
  REPLACEMENT: 'Thay đồng hồ', NEGATIVE_READING: 'Chỉ số âm', MISSING_PREDECESSOR: 'Thiếu chỉ số ngày trước',
  INVALID_PREDECESSOR: 'Chỉ số trước không hợp lệ', NEGATIVE_DELTA: 'Chỉ số giảm', FEWER_THAN_28_VALID_PRIOR_POINTS: 'Chưa có đủ 28 điểm hợp lệ trước đó',
}
