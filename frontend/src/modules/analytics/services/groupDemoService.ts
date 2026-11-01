import { z } from 'zod'
import apiClient from '../../../core/api/client'

export const GROUP_SCENARIOS = [
  ['FIT_AND_TIE', 'Đủ sức chứa, ưu tiên giường và tầng', 'P01', 3],
  ['QUOTA_ONE', 'Quota chỉ đủ một phòng', 'P11_QUOTA_ONE', 3],
  ['MIDDLE_NIGHT_LIMIT', 'Giới hạn ở đêm giữa', 'P12_MIDDLE_BOTTLENECK', 4],
  ['ZERO_QUOTA', 'Quota bằng 0', 'P14_ZERO', 4],
  ['QUOTA_CONTROL', 'Quota đủ cho hai yêu cầu', 'P14_CONTROL', 4],
  ['NO_TRANSFER_BLOCK', 'Không chuyển quota giữa block', 'P14_NO_TRANSFER_roomBlockId', 3],
  ['NO_TRANSFER_ROOM_TYPE', 'Không chuyển quota giữa loại phòng', 'P14_NO_TRANSFER_hotelRoomTypeId', 3],
] as const
const scenario = z.enum(['FIT_AND_TIE', 'QUOTA_ONE', 'MIDDLE_NIGHT_LIMIT', 'ZERO_QUOTA', 'QUOTA_CONTROL', 'NO_TRANSFER_BLOCK', 'NO_TRANSFER_ROOM_TYPE'])
export type GroupScenario = z.infer<typeof scenario>
const positive = z.number().int().min(1).max(2_147_483_647)
const nonnegative = z.number().int().min(0).max(Number.MAX_SAFE_INTEGER)
const key = z.string().regex(/^[A-Za-z0-9_-]{1,64}$/)
const date = z.string().regex(/^2026-11-0[1-4]$/)
const reference = z.literal('2026-10-31T02:00:00Z')
const warnings = ['UNKNOWN_FLOOR_METADATA', 'EXCLUDED_ROOM_UNAVAILABLE', 'EXCLUDED_BLOCK_INACTIVE', 'EXCLUDED_CUTOFF_EXPIRED'] as const
const schema = z.object({
  dataMode: z.literal('SYNTHETIC'), businessData: z.literal(false), previewOnly: z.literal(true), scenario,
  notice: z.string().min(1).max(500), referenceInstant: reference, allDemandsAssigned: z.boolean(),
  scenarios: z.array(z.object({ id: scenario, label: z.string().min(1).max(100) })).length(7),
  request: z.object({ hotelId: z.literal(1), contractId: z.literal(501), checkInDate: z.literal('2026-11-01'), checkOutDate: date,
    demands: z.array(z.object({ demandKey: key, persons: positive, requiredBedUnits: positive })).min(1).max(100) }),
  snapshot: z.object({
    scope: z.object({ hotelId: z.literal(1), regionId: z.literal(10), contractId: z.literal(501) }),
    snapshotId: z.string().min(1).max(128), observedAt: z.literal('2026-10-31T01:59:55Z'), validUntil: z.literal('2026-10-31T02:00:55Z'),
    basis: z.literal('SYNTHETIC_PREVIEWABLE_ROOMS_V1'), dataMode: z.literal('SYNTHETIC'), coverage: z.literal('COMPLETE'),
    candidates: z.array(z.object({ roomInstanceId: positive, hotelId: z.literal(1), roomBlockId: positive, hotelRoomTypeId: positive,
      capacityPersons: positive, bedUnits: positive, floorNumber: z.number().int().min(-2_147_483_648).max(2_147_483_647).nullable(), eligibleForEntireStay: z.boolean() })).max(300),
    quotaRows: z.array(z.object({ hotelId: z.literal(1), contractId: z.literal(501), roomBlockId: positive, hotelRoomTypeId: positive,
      stayDate: date, previewableRoomCount: z.number().int().min(0).max(2_147_483_647) })).max(9300),
    nightEligibility: z.array(z.object({ roomInstanceId: positive, hotelId: z.literal(1), contractId: z.literal(501), roomBlockId: positive,
      hotelRoomTypeId: positive, stayDate: date, state: z.enum(['ELIGIBLE', 'ROOM_UNAVAILABLE', 'BLOCK_INACTIVE', 'CUTOFF_EXPIRED']) })).max(9300),
  }),
  preview: z.object({ status: z.literal('COMPLETE'), reasonCode: z.null(), dataMode: z.literal('SYNTHETIC'), hotelId: z.literal(1), contractId: z.literal(501),
    checkInDate: date, checkOutDate: date, observedAt: z.literal('2026-10-31T01:59:55Z'), previewOnly: z.literal(true),
    assignments: z.array(z.object({ demandKey: key, roomInstanceId: positive })).max(100),
    unmet: z.array(z.object({ demandKey: key, reason: z.literal('NO_ELIGIBLE_CAPACITY') })).max(100),
    warnings: z.array(z.enum(warnings)).max(4),
    objective: z.object({ assignedCount: z.number().int().min(0).max(100), bedWaste: nonnegative,
      floorSpan: nonnegative.nullable(), floorObjectiveEnabled: z.boolean() }),
  }),
})
export type GroupDemo = z.infer<typeof schema>
export const groupDate = (value: string) => value.split('-').reverse().join('/')
export const groupWarning: Record<typeof warnings[number], string> = {
  UNKNOWN_FLOOR_METADATA: 'Thiếu thông tin tầng; không đánh giá độ trải tầng.',
  EXCLUDED_ROOM_UNAVAILABLE: 'Có phòng bị loại vì không đủ điều kiện trong kỳ lưu trú.',
  EXCLUDED_BLOCK_INACTIVE: 'Có block không hoạt động trong kỳ lưu trú.',
  EXCLUDED_CUTOFF_EXPIRED: 'Có block hết hạn cutoff trong kỳ lưu trú.',
}

// Sanity-check the returned plan and its bounded snapshot; never search or rank rooms here.
function validateRelations(data: GroupDemo, selected: GroupScenario) {
  const { request, snapshot, preview } = data
  const meta = GROUP_SCENARIOS.find(row => row[0] === selected)!
  const reject = () => { throw new Error('Invalid synthetic Group preview') }
  if (data.scenario !== selected || snapshot.snapshotId !== `synthetic-${meta[2]}`
    || request.checkOutDate !== `2026-11-0${meta[3]}` || preview.checkInDate !== request.checkInDate || preview.checkOutDate !== request.checkOutDate
    || data.scenarios.some((row, i) => row.id !== GROUP_SCENARIOS[i][0])) reject()
  const nights = Array.from({ length: meta[3] - 1 }, (_, i) => `2026-11-0${i + 1}`)
  const rooms = new Map(snapshot.candidates.map(room => [room.roomInstanceId, room]))
  const demands = new Map(request.demands.map(demand => [demand.demandKey, demand]))
  const block = (room: { roomBlockId: number; hotelRoomTypeId: number }) => `${room.roomBlockId}/${room.hotelRoomTypeId}`
  const blocks = new Set(snapshot.candidates.map(block))
  const quota = new Map(snapshot.quotaRows.map(row => [`${block(row)}/${row.stayDate}`, row.previewableRoomCount]))
  const eligibility = new Map(snapshot.nightEligibility.map(row => [`${row.roomInstanceId}/${row.stayDate}`, row.state]))
  if (rooms.size !== snapshot.candidates.length || demands.size !== request.demands.length || quota.size !== snapshot.quotaRows.length
    || eligibility.size !== snapshot.nightEligibility.length || quota.size !== blocks.size * nights.length || eligibility.size !== rooms.size * nights.length
    || snapshot.quotaRows.some(row => !blocks.has(block(row)) || !nights.includes(row.stayDate))
    || snapshot.nightEligibility.some(row => { const room = rooms.get(row.roomInstanceId); return !room || block(room) !== block(row) || !nights.includes(row.stayDate) })
    || snapshot.candidates.some(room => room.eligibleForEntireStay !== nights.every(night => eligibility.get(`${room.roomInstanceId}/${night}`) === 'ELIGIBLE'))) reject()
  const keys = [...preview.assignments, ...preview.unmet].map(row => row.demandKey)
  if (new Set(keys).size !== keys.length || keys.length !== demands.size || keys.some(k => !demands.has(k))
    || new Set(preview.assignments.map(row => row.roomInstanceId)).size !== preview.assignments.length
    || preview.objective.assignedCount !== preview.assignments.length || data.allDemandsAssigned !== (preview.unmet.length === 0)) reject()
  const assignedBlocks = new Map<string, number>()
  let waste = 0
  const floors: number[] = []
  for (const assignment of preview.assignments) {
    const room = rooms.get(assignment.roomInstanceId), demand = demands.get(assignment.demandKey)
    if (!room || !demand || !room.eligibleForEntireStay || room.capacityPersons < demand.persons || room.bedUnits < demand.requiredBedUnits) return reject()
    waste += room.bedUnits - demand.requiredBedUnits
    if (room.floorNumber !== null) floors.push(room.floorNumber)
    assignedBlocks.set(block(room), (assignedBlocks.get(block(room)) ?? 0) + 1)
  }
  for (const [id, count] of assignedBlocks) if (nights.some(night => (quota.get(`${id}/${night}`) ?? -1) < count)) reject()
  const floorEnabled = snapshot.candidates.filter(room => room.eligibleForEntireStay).every(room => room.floorNumber !== null)
  const expectedWarnings = new Set<string>(snapshot.nightEligibility.filter(row => row.state !== 'ELIGIBLE').map(row => `EXCLUDED_${row.state}`))
  if (!floorEnabled) expectedWarnings.add('UNKNOWN_FLOOR_METADATA')
  if (preview.objective.bedWaste !== waste || preview.objective.floorObjectiveEnabled !== floorEnabled
    || preview.objective.floorSpan !== (floorEnabled ? floors.length ? Math.max(...floors) - Math.min(...floors) : 0 : null)
    || preview.warnings.length !== expectedWarnings.size || preview.warnings.some(warning => !expectedWarnings.has(warning))) reject()
}

export const groupDemoService = {
  async get(selected: GroupScenario, signal: AbortSignal): Promise<GroupDemo> {
    scenario.parse(selected)
    const raw = await apiClient.get<unknown>('/api/v1/ai/demo/group-room-assignment', { params: { scenario: selected }, signal })
    const data = z.object({ code: z.literal(200), result: schema }).parse(raw).result
    validateRelations(data, selected)
    return data
  },
}
