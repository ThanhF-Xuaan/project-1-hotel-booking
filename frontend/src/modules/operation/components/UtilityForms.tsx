import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import type { MeterCreate, ReadingCreate, UtilityMeter, UtilityReading } from '../types/utility.types'

const meterSchema = z.object({
  meterCode: z.string().trim().min(1, 'Nhập mã đồng hồ').max(50, 'Mã tối đa 50 ký tự'),
  meterType: z.enum(['ELECTRICITY', 'WATER']),
  locationLabel: z.string().trim().max(100, 'Vị trí tối đa 100 ký tự'),
})

const vietnamToday = () => new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date())

const readingSchema = z.object({
  readingDate: z.string().date().refine((date: string) => date <= vietnamToday(), 'Ngày ghi không được ở tương lai'),
  readingValue: z.string().trim().regex(/^\d{1,9}(?:[.,]\d{1,3})?$/, 'Nhập số không âm, tối đa 9 chữ số nguyên và 3 chữ số thập phân'),
  isMeterReset: z.boolean(),
})

export const selectClass = 'w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm focus-visible:outline-red-600'

interface FormActions {
  busy: boolean
  uncertain: boolean
  error?: string
  close: () => void
}

function FormFooter({ busy, uncertain, error, close }: FormActions) {
  return <>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    <div className="flex justify-end gap-2">
      <Button type="button" variant="outline" disabled={busy} onClick={close}>Đóng</Button>
      <Button type="submit" isLoading={busy} disabled={uncertain}>Lưu</Button>
    </div>
  </>
}

export function MeterForm({ meter, hotelId, save, ...actions }: FormActions & {
  meter?: UtilityMeter
  hotelId: number
  save: (data: MeterCreate) => Promise<void>
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof meterSchema>>({
    resolver: zodResolver(meterSchema),
    defaultValues: {
      meterCode: meter?.meterCode ?? '',
      meterType: meter?.meterType ?? 'ELECTRICITY',
      locationLabel: meter?.locationLabel ?? '',
    },
  })
  return <form noValidate className="space-y-4" onSubmit={handleSubmit(values => save({
    ...values, hotelId, locationLabel: values.locationLabel || null,
  }))}>
    <fieldset disabled={actions.busy} className="space-y-4">
      <Input id="utility-meter-code" label="Mã đồng hồ" error={errors.meterCode?.message} {...register('meterCode')} />
      <label className="block text-sm font-medium">
        Loại đồng hồ
        {meter ? <p>{meter.meterType === 'ELECTRICITY' ? 'Điện' : 'Nước'}</p> : (
          <select className={selectClass} {...register('meterType')}>
            <option value="ELECTRICITY">Điện</option>
            <option value="WATER">Nước</option>
          </select>
        )}
      </label>
      <Input id="utility-meter-location" label="Vị trí" error={errors.locationLabel?.message} {...register('locationLabel')} />
    </fieldset>
    <FormFooter {...actions} />
  </form>
}

export function ReadingForm({ reading, meter, save, ...actions }: FormActions & {
  reading?: UtilityReading
  meter: UtilityMeter
  save: (data: ReadingCreate) => Promise<void>
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof readingSchema>>({
    resolver: zodResolver(readingSchema),
    defaultValues: {
      readingDate: reading?.readingDate ?? vietnamToday(),
      readingValue: reading ? String(reading.readingValue) : '',
      isMeterReset: reading?.isMeterReset ?? false,
    },
  })
  return <form noValidate className="space-y-4" onSubmit={handleSubmit(values => save({
    ...values, meterId: meter.id, readingValue: Number(values.readingValue.replace(',', '.')),
  }))}>
    <fieldset disabled={actions.busy} className="space-y-4">
      <p className="text-sm">Đồng hồ <strong>{meter.meterCode}</strong></p>
      <Input id="utility-reading-date" label="Ngày ghi" type="date" max={vietnamToday()}
        readOnly={!!reading} error={errors.readingDate?.message} {...register('readingDate')} />
      <Input id="utility-reading-value" label="Chỉ số" inputMode="decimal"
        error={errors.readingValue?.message} {...register('readingValue')} />
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" {...register('isMeterReset')} />
        Đã đặt lại đồng hồ
      </label>
    </fieldset>
    <FormFooter {...actions} />
  </form>
}
