import { z } from 'zod'
import apiClient from '../../../core/api/client'

export type DemoScenario = 'HOTEL_A' | 'HOTEL_B'
const date = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine(value =>
  Number.isFinite(Date.parse(`${value}T00:00:00Z`)) && new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value)
const percent = z.number().min(0).max(100)
const sample = z.object({
  hotelId: z.number().int().positive(), date,
  occupiedRooms: z.number().int().nonnegative().nullable(),
  capacityRooms: z.number().int().nonnegative().nullable(),
  coverage: z.enum(['COMPLETE', 'MISSING']),
}).refine(row => row.coverage === 'MISSING'
  ? row.occupiedRooms === null && row.capacityRooms === null
  : row.occupiedRooms !== null && row.capacityRooms !== null && row.occupiedRooms <= row.capacityRooms)
const point = z.object({ date, occupancyPercent: percent, lowerPercent: percent, upperPercent: percent })
  .refine(row => row.lowerPercent <= row.occupancyPercent && row.occupancyPercent <= row.upperPercent)
const reference = z.literal('2026-10-29T17:00:00Z')
const responseSchema = z.object({
  dataMode: z.literal('SYNTHETIC'), businessData: z.literal(false),
  scenario: z.enum(['HOTEL_A', 'HOTEL_B']), referenceInstant: reference,
  history: z.object({
    status: z.enum(['COMPLETE', 'PARTIAL']), hotelId: z.number().int().positive(),
    fromInclusive: date, toInclusive: date, asOf: reference, dataMode: z.literal('SYNTHETIC'),
    samples: z.array(sample).min(1).max(500),
  }),
  forecast: z.object({
    status: z.enum(['FORECAST_READY', 'BASELINE_FALLBACK', 'INSUFFICIENT_COVERAGE', 'INVALID_REQUEST', 'ACCESS_DENIED', 'DEPENDENCY_UNAVAILABLE']),
    hotelId: z.number().int().positive(), asOf: reference.nullable(), dataMode: z.literal('SYNTHETIC').nullable(),
    model: z.enum(['SEASONAL_NAIVE_7', 'RIDGE_TREND_WEEKDAY']).nullable(),
    modelVersion: z.string().max(80).nullable(),
    rangeMethod: z.enum(['ROLLING_OUT_OF_SAMPLE_ABS_ERROR_P90', 'CAUSAL_WEEKLY_DIFFERENCE_ABS_ERROR_P90']).nullable(),
    residualRadiusPp: z.number().finite().nonnegative().nullable(),
    points: z.array(point).max(30),
  }),
})

export type OccupancyDemo = z.infer<typeof responseSchema>
export type OccupancySample = OccupancyDemo['history']['samples'][number]
export type ForecastPoint = OccupancyDemo['forecast']['points'][number]

function nextDay(value: string): string {
  return new Date(Date.parse(`${value}T00:00:00Z`) + 86400000).toISOString().slice(0, 10)
}

function validateResponse(raw: unknown, scenario: DemoScenario): OccupancyDemo {
  const value = responseSchema.parse(raw)
  const { history, forecast } = value
  const ready = forecast.status === 'FORECAST_READY' || forecast.status === 'BASELINE_FALLBACK'
  if (value.scenario !== scenario || history.hotelId !== (scenario === 'HOTEL_A' ? 1 : 2)
    || forecast.hotelId !== history.hotelId || history.toInclusive !== '2026-10-29'
    || history.samples[0].date !== history.fromInclusive || history.samples.at(-1)?.date !== history.toInclusive
    || history.samples.some((row, i, rows) => row.hotelId !== history.hotelId || (i > 0 && row.date !== nextDay(rows[i - 1].date)))
    || (history.status === 'COMPLETE' && history.samples.some(row => row.coverage === 'MISSING'))
    || (ready && (forecast.points.length !== 30 || !forecast.model || !forecast.modelVersion || !forecast.rangeMethod
      || forecast.residualRadiusPp === null || forecast.dataMode !== 'SYNTHETIC' || forecast.asOf !== value.referenceInstant
      || forecast.points.some((row, i, rows) => row.date !== nextDay(i === 0 ? history.toInclusive : rows[i - 1].date))))
    || (!ready && (forecast.points.length !== 0 || forecast.model !== null || forecast.residualRadiusPp !== null))) {
    throw new Error('Invalid synthetic replay response')
  }
  return value
}

export const occupancyDemoService = {
  async get(scenario: DemoScenario, signal: AbortSignal): Promise<OccupancyDemo> {
    const response = await apiClient.get<unknown>('/api/v1/ai/demo/occupancy', { params: { scenario }, signal })
    if (response.code !== 200) throw new Error('Invalid demo envelope')
    return validateResponse(response.result, scenario)
  },
}

export function occupancyPercent(row: OccupancySample): number | null {
  return row.coverage === 'COMPLETE' && row.capacityRooms !== null && row.capacityRooms > 0 && row.occupiedRooms !== null
    ? 100 * row.occupiedRooms / row.capacityRooms : null
}

export function displayDate(value: string): string {
  return value.split('-').reverse().join('/')
}
