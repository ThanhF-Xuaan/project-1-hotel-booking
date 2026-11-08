import { useRef, useState } from 'react'
import { Download, Upload } from 'lucide-react'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import { ConfirmModal } from '../../../core/components/ui/Modal'
import { downloadFile } from '../../../core/utils/downloadFile'
import { importPayroll, PAYROLL_CSV_HEADER, payrollIssueLabel, type PayrollImportIssue } from '../services/payroll.service'

export default function PayrollImportPage() {
  const [file, setFile] = useState<File>()
  const [confirm, setConfirm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [issues, setIssues] = useState<PayrollImportIssue[]>()
  const [error, setError] = useState('')
  const [needsReconciliation, setNeedsReconciliation] = useState(false)
  const lock = useRef(false)
  const fileInput = useRef<HTMLInputElement>(null)

  async function submit() {
    if (!file || lock.current || needsReconciliation) return
    lock.current = true
    setBusy(true)
    setError('')
    setIssues(undefined)
    try {
      setIssues(await importPayroll(file))
      setFile(undefined)
      if (fileInput.current) fileInput.current.value = ''
    } catch (reason) {
      const status = getErrorStatus(reason)
      const uncertain = status == null || status >= 500
      setNeedsReconciliation(uncertain)
      setError(uncertain
        ? 'Chưa xác định được kết quả nhập. Đối chiếu kỳ lương với người phụ trách trước khi gửi lại.'
        : status === 413 ? 'File vượt giới hạn tải lên của máy chủ.'
          : status === 403 ? 'Tài khoản không có quyền nhập bảng lương.'
            : 'Máy chủ từ chối file CSV. Kiểm tra file và thử lại.')
    } finally {
      lock.current = false
      setBusy(false)
      setConfirm(false)
    }
  }

  const fileIssues = issues?.filter(issue => issue.row === 0 || issue.row === 1) ?? []
  const rowIssues = issues?.filter(issue => issue.row > 1) ?? []
  return <div className="max-w-3xl space-y-5">
    <header><h1 className="text-2xl font-bold text-neutral-900">Nhập bảng lương</h1>
      <p className="mt-1 text-sm text-neutral-600">Tải một file CSV để thêm hoặc cập nhật bảng lương theo kỳ.</p>
    </header>
    <section className="space-y-4 rounded-xl border border-neutral-200 bg-white p-4">
      <Button variant="outline" onClick={() => downloadFile(new Blob([PAYROLL_CSV_HEADER + '\r\n'],
        { type: 'text/csv;charset=utf-8' }), 'mau-nhap-bang-luong.csv')}>
        <Download className="h-4 w-4" aria-hidden="true" /> Tải CSV mẫu
      </Button>
      <Input ref={fileInput} id="payroll-file" type="file" label="File bảng lương (.csv)" accept=".csv,text/csv"
        disabled={busy || needsReconciliation} onChange={event => {
          const selected = event.target.files?.[0]
          setIssues(undefined)
          setError('')
          if (selected && (!/\.csv$/i.test(selected.name) || !selected.size)) {
            setFile(undefined)
            event.target.value = ''
            setError('Chọn một file CSV có nội dung.')
          } else setFile(selected)
        }} />
      {file && <p className="break-all text-sm text-neutral-700">{file.name} · {new Intl.NumberFormat('vi-VN').format(file.size)} byte</p>}
      <Button disabled={!file || busy || needsReconciliation} onClick={() => setConfirm(true)}>
        <Upload className="h-4 w-4" aria-hidden="true" /> Nhập bảng lương
      </Button>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {needsReconciliation && <Button variant="outline" onClick={() => {
        setNeedsReconciliation(false)
        setError('')
      }}>Đã đối chiếu kỳ lương</Button>}
    </section>
    {issues && <section className="space-y-3 rounded-xl border border-neutral-200 bg-white p-4" aria-label="Kết quả nhập" aria-live="polite">
      <h2 className="font-semibold">{fileIssues.length ? 'Lỗi cấp file'
        : rowIssues.length ? 'Hoàn tất có lỗi' : 'Nhập hoàn tất'}</h2>
      {fileIssues.map((issue, index) => <p key={index} role="alert" className="text-sm text-red-700">{payrollIssueLabel(issue)}</p>)}
      {!!rowIssues.length && <>
        <p className="text-sm text-neutral-600">Sửa các dòng bên dưới và giữ nguyên dòng tiêu đề khi gửi lại. Các dòng hợp lệ có thể đã được ghi.</p>
        <div className="max-h-96 overflow-auto"><table className="w-full text-left text-sm">
          <thead className="bg-neutral-50"><tr><th className="p-2" scope="col">Dòng</th><th className="p-2" scope="col">Lỗi</th></tr></thead>
          <tbody className="divide-y divide-neutral-100">{rowIssues.map((issue, index) => <tr key={index}>
            <th scope="row" className="p-2">{issue.row}</th><td className="p-2">{payrollIssueLabel(issue)}</td>
          </tr>)}</tbody>
        </table></div>
      </>}
    </section>}
    {confirm && <ConfirmModal isOpen title="Xác nhận nhập bảng lương" variant="warning" isLoading={busy}
      description="Dòng hợp lệ có thể cập nhật số liệu đã có của cùng nhân viên và kỳ lương."
      confirmText="Xác nhận nhập" onConfirm={submit} onClose={() => { if (!lock.current) setConfirm(false) }} />}
  </div>
}
