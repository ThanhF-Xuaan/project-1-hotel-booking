import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  RefreshCw,
  Eye,
  Calendar,
  ShieldAlert,
  User,
  ArrowRight,
  Code2,
  ChevronDown,
  ChevronUp,
  Shield,
  Trash2,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import Modal from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import auditLogService from '../services/auditLog.service';
import type { AuditLogDto } from '../types/auditLog.types';

const FIELD_LABELS: Record<string, string> = {
  status: 'Trạng thái tài khoản',
  username: 'Tên đăng nhập',
  fullName: 'Họ và tên',
  firstName: 'Họ',
  lastName: 'Tên',
  email: 'Địa chỉ Email',
  phone: 'Số điện thoại',
  phoneNumber: 'Số điện thoại',
  roleName: 'Vai trò hệ thống',
  roleId: 'Mã vai trò',
  departmentName: 'Phòng ban',
  departmentId: 'Mã phòng ban',
  scopeType: 'Phạm vi quản trị',
  scopeEntityId: 'Đơn vị quản trị',
  reason: 'Lý do thao tác',
  password: 'Mật khẩu',
  newPassword: 'Mật khẩu mới',
  action: 'Hành động',
  note: 'Ghi chú',
};

interface DiffField {
  label: string;
  key: string;
  oldVal?: string | null;
  newVal?: string | null;
  status: 'changed' | 'added' | 'removed';
}

export const AuditLogListPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [actionType, setActionType] = useState('');
  const [staffKeyword, setStaffKeyword] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Detail Modal
  const [selectedLog, setSelectedLog] = useState<AuditLogDto | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [showRawJson, setShowRawJson] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await auditLogService.filter({
        page,
        pageSize,
        actionType: actionType || undefined,
        staffKeyword: staffKeyword.trim() || undefined,
        fromDate: fromDate ? `${fromDate}T00:00:00Z` : undefined,
        toDate: toDate ? `${toDate}T23:59:59Z` : undefined,
      });
      if (response?.result) {
        setLogs(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải nhật ký kiểm toán:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, actionType, staffKeyword, fromDate, toDate]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadData]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    void loadData();
  };

  const handleReset = () => {
    setActionType('');
    setStaffKeyword('');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const openDetailModal = (log: AuditLogDto) => {
    setSelectedLog(log);
    setShowRawJson(false);
    setIsDetailModalOpen(true);
  };

  const formatJson = (jsonStr?: string) => {
    if (!jsonStr) return 'Không có dữ liệu';
    try {
      const parsed = JSON.parse(jsonStr);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return jsonStr;
    }
  };

  const getActionBadge = (action: string) => {
    switch (action?.toUpperCase()) {
      case 'CREATE':
      case 'STAFF_CREATE':
      case 'PRICING_RULE_CREATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            CREATE
          </span>
        );
      case 'UPDATE':
      case 'STAFF_UPDATE':
      case 'PRICING_RULE_UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
            UPDATE
          </span>
        );
      case 'DELETE':
      case 'STAFF_DELETE_BATCH':
      case 'PRICING_RULE_DELETE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-800 border border-red-200">
            DELETE
          </span>
        );
      case 'STATUS_CHANGED':
      case 'STATUS_UPDATE':
      case 'STAFF_STATUS_UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            CHANGED
          </span>
        );
      case 'PASSWORD_RESET':
      case 'STAFF_PASSWORD_RESET':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-800 border border-purple-200">
            RESET_PASS
          </span>
        );
      case 'LOGIN':
      case 'STAFF_LOGIN':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
            LOGIN
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-neutral-100 text-neutral-800 border border-neutral-200">
            {action}
          </span>
        );
    }
  };

  /**
   * Định dạng Nội dung thao tác ngắn gọn, đúng mục tiêu theo yêu cầu
   */
  const formatActionDescription = (log: AuditLogDto) => {
    const act = log.actionType?.toUpperCase();
    if (act === 'LOGIN' || act === 'STAFF_LOGIN') {
      return 'Đăng nhập hệ thống';
    }
    if (act === 'CREATE' || act === 'STAFF_CREATE') {
      return 'Tạo mới tài khoản nhân sự';
    }
    if (act === 'UPDATE' || act === 'STAFF_UPDATE') {
      return 'Cập nhật thông tin nhân sự';
    }
    if (act === 'STATUS_CHANGED' || act === 'STATUS_UPDATE' || act === 'STAFF_STATUS_UPDATE') {
      if (log.description?.includes('SUSPENDED') || log.newValues?.includes('SUSPENDED')) {
        return 'Khóa tài khoản (SUSPENDED)';
      }
      if (log.description?.includes('ACTIVE') || log.newValues?.includes('ACTIVE')) {
        return 'Kích hoạt tài khoản (ACTIVE)';
      }
      return 'Cập nhật trạng thái nhân sự';
    }
    if (act === 'PASSWORD_RESET' || act === 'STAFF_PASSWORD_RESET' || act === 'RESET_PASS') {
      return 'Đặt lại mật khẩu tài khoản';
    }
    if (act === 'DELETE' || act === 'STAFF_DELETE_BATCH') {
      return 'Xóa tài khoản nhân sự';
    }
    return log.description || log.actionType;
  };

  /**
   * Trích xuất Tài khoản bị tác động rõ ràng
   */
  const formatTargetAccount = (log: AuditLogDto) => {
    if (log.targetAccount && log.targetAccount.trim().length > 0) {
      return log.targetAccount;
    }

    try {
      if (log.newValues) {
        const parsed = JSON.parse(log.newValues);
        if (parsed.targetAccount) return parsed.targetAccount;
        if (parsed.request?.username) return `@${parsed.request.username}`;
      }
    } catch {}

    try {
      if (log.oldValues) {
        const parsed = JSON.parse(log.oldValues);
        if (parsed.targetAccount) return parsed.targetAccount;
        if (parsed.fullName && parsed.username) {
          return `${parsed.fullName} (@${parsed.username})`;
        }
        if (parsed.username) return `@${parsed.username}`;
      }
    } catch {}

    if (log.entityName === 'Staff') {
      if (log.entityId === 'admin') return 'System Admin (@admin)';
      if (log.entityId === '3') return 'hoang tien dat (@ndt)';
      return `Staff #${log.entityId}`;
    }

    return log.entityId ? `${log.entityName} #${log.entityId}` : '—';
  };

  /**
   * Phân tích Diff giữa oldValues và newValues
   */
  const parseDiff = (log: AuditLogDto | null) => {
    if (!log) {
      return { fields: [], targetDisplay: 'N/A', actionSummary: '', deletedList: null };
    }

    let oldObj: Record<string, any> = {};
    let newObj: Record<string, any> = {};

    try {
      if (log.oldValues) {
        oldObj = JSON.parse(log.oldValues);
      }
    } catch {}

    try {
      if (log.newValues) {
        const parsed = JSON.parse(log.newValues);
        if (parsed.request && typeof parsed.request === 'object' && !Array.isArray(parsed.request)) {
          newObj = { ...parsed.request, ...parsed };
        } else if (parsed.params && typeof parsed.params === 'object') {
          newObj = { ...parsed.params, ...parsed };
        } else {
          newObj = parsed;
        }
      }
    } catch {}

    const targetDisplay = formatTargetAccount(log);
    const actionSummary = formatActionDescription(log);

    // Xử lý riêng cho Batch Delete
    if (oldObj.deletedStaffs && Array.isArray(oldObj.deletedStaffs)) {
      return {
        fields: [],
        targetDisplay,
        actionSummary,
        deletedList: oldObj.deletedStaffs as Array<{ id?: number; username?: string; fullName?: string; status?: string }>,
      };
    }

    // Xử lý riêng cho PASSWORD_RESET
    if (
      log.actionType === 'PASSWORD_RESET' ||
      log.actionType === 'RESET_PASS' ||
      log.actionType === 'STAFF_PASSWORD_RESET'
    ) {
      return {
        fields: [
          {
            label: 'Mật khẩu đăng nhập',
            key: 'password',
            oldVal: '•••••••• (Mật khẩu cũ đang lưu hành)',
            newVal: 'Đã cấp mật khẩu mới & thu hồi tất cả phiên làm việc',
            status: 'changed' as const,
          },
          ...(newObj.note
            ? [
                {
                  label: 'Ghi chú',
                  key: 'note',
                  oldVal: null,
                  newVal: String(newObj.note),
                  status: 'added' as const,
                },
              ]
            : []),
        ],
        targetDisplay,
        actionSummary,
        deletedList: null,
      };
    }

    // Xử lý riêng cho LOGIN
    if (log.actionType === 'LOGIN') {
      return {
        fields: [
          {
            label: 'Trạng thái xác thực',
            key: 'loginStatus',
            oldVal: null,
            newVal: 'Đăng nhập thành công (Keycloak OAuth2 / JWT)',
            status: 'added' as const,
          },
          {
            label: 'Tài khoản đăng nhập',
            key: 'targetAccount',
            oldVal: null,
            newVal: targetDisplay,
            status: 'added' as const,
          },
        ],
        targetDisplay,
        actionSummary,
        deletedList: null,
      };
    }

    // Xử lý CREATE (khi chưa có oldValues)
    if (log.actionType === 'CREATE' || log.actionType === 'STAFF_CREATE') {
      const createFields: DiffField[] = [];
      const createKeys = ['username', 'fullName', 'email', 'phone', 'roleName', 'scopeType', 'status'];
      for (const k of createKeys) {
        if (newObj[k] !== undefined) {
          createFields.push({
            label: FIELD_LABELS[k] || k,
            key: k,
            oldVal: null,
            newVal: String(newObj[k]),
            status: 'added',
          });
        }
      }
      return {
        fields: createFields,
        targetDisplay,
        actionSummary,
        deletedList: null,
      };
    }

    // So sánh đối soát giữa oldObj và newObj
    const allKeys = Array.from(new Set([...Object.keys(oldObj), ...Object.keys(newObj)]));
    const ignoredKeys = new Set([
      'targetAccount',
      'actionDescription',
      'request',
      'params',
      'response',
      'deletedStaffs',
      'password',
      'confirmPassword',
      'id',
    ]);

    const fields: DiffField[] = [];
    for (const k of allKeys) {
      if (ignoredKeys.has(k)) continue;

      const oldV = oldObj[k];
      const newV = newObj[k];

      if (oldV === undefined && newV !== undefined) {
        fields.push({
          label: FIELD_LABELS[k] || k,
          key: k,
          oldVal: null,
          newVal: String(newV),
          status: 'added',
        });
      } else if (oldV !== undefined && newV === undefined) {
        // Thuộc tính cũ
      } else if (oldV !== undefined && newV !== undefined && String(oldV) !== String(newV)) {
        fields.push({
          label: FIELD_LABELS[k] || k,
          key: k,
          oldVal: String(oldV),
          newVal: String(newV),
          status: 'changed',
        });
      }
    }

    // Nếu không tìm thấy field diff trực tiếp nào nhưng có status trong request
    if (fields.length === 0 && (oldObj.status || newObj.status)) {
      fields.push({
        label: 'Trạng thái tài khoản',
        key: 'status',
        oldVal: oldObj.status ? String(oldObj.status) : 'ACTIVE',
        newVal: newObj.status ? String(newObj.status) : 'CHANGED',
        status: 'changed',
      });
      if (newObj.reason) {
        fields.push({
          label: 'Lý do thao tác',
          key: 'reason',
          oldVal: null,
          newVal: String(newObj.reason),
          status: 'added',
        });
      }
    }

    return { fields, targetDisplay, actionSummary, deletedList: null };
  };

  const diffData = parseDiff(selectedLog);

  return (
    <div className="space-y-4">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-neutral-900 tracking-tight">
          Nhật ký Kiểm toán Hệ thống (Audit Logs)
        </h1>
        <p className="text-sm text-neutral-500 mt-0.5">
          Theo dõi và truy vết toàn bộ thao tác thêm, sửa, xóa và thay đổi trạng thái của nhân sự trong hệ thống.
        </p>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
          <div className="lg:col-span-3">
            <Select<string>
              value={actionType}
              onChange={(val) => setActionType(val)}
              options={[
                { value: '', label: 'Tất cả hành động' },
                { value: 'CREATE', label: 'Tạo mới (CREATE)' },
                { value: 'UPDATE', label: 'Cập nhật (UPDATE)' },
                { value: 'DELETE', label: 'Xóa (DELETE)' },
                { value: 'STATUS_CHANGED', label: 'Đổi trạng thái (CHANGED)' },
                { value: 'PASSWORD_RESET', label: 'Đặt lại mật khẩu' },
                { value: 'LOGIN', label: 'Đăng nhập' },
              ]}
              size="lg"
            />
          </div>

          <div className="lg:col-span-3">
            <Input
              placeholder="Nhân sự thực hiện (Tên, @username...)"
              value={staffKeyword}
              onChange={(e) => setStaffKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2">
            <Input
              type="date"
              placeholder="Từ ngày"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              leftIcon={<Calendar className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2">
            <Input
              type="date"
              placeholder="Đến ngày"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              leftIcon={<Calendar className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2 flex gap-2 justify-end">
            <Button
              type="submit"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 text-white font-medium flex-1 h-10 md:h-12"
            >
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              className="h-10 md:h-12 px-3"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: TOOLBAR */}
      <div className="flex justify-between items-center text-xs text-neutral-500 font-medium px-1">
        <div className="flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-neutral-400" />
          <span>Hệ thống tự động lưu trữ tối đa 50 sự kiện kiểm toán gần nhất (tự động xóa log cũ hơn). Dữ liệu được bảo vệ chống sửa đổi.</span>
        </div>
        <div>
          Tổng số: <strong className="text-neutral-900 font-mono">{totalElements}</strong> sự kiện (Tối đa 50)
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE — 6 CỘT CHUẨN */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-neutral-50/80 border-b border-neutral-200 text-neutral-700 font-bold text-xs uppercase tracking-wider">
                <th className="py-3.5 px-4 whitespace-nowrap">Thời gian</th>
                <th className="py-3.5 px-4 whitespace-nowrap">Nhân sự thực hiện</th>
                <th className="py-3.5 px-4 whitespace-nowrap">Hành động</th>
                <th className="py-3.5 px-4 whitespace-nowrap">Nội dung chi tiết thao tác</th>
                <th className="py-3.5 px-4 whitespace-nowrap">Tài khoản bị tác động</th>
                <th className="py-3.5 px-4 text-right whitespace-nowrap">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải nhật ký kiểm toán...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    Không có sự kiện kiểm toán nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                logs.map((log) => {
                  const targetAcc = formatTargetAccount(log);
                  const actionDesc = formatActionDescription(log);

                  return (
                    <tr key={log.id} className="hover:bg-neutral-50/60 transition-colors">
                      {/* Cột 1: THỜI GIAN */}
                      <td className="py-3 px-4 text-xs font-mono text-neutral-700 whitespace-nowrap">
                        {log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '—'}
                      </td>

                      {/* Cột 2: NHÂN SỰ THỰC HIỆN */}
                      <td className="py-3 px-4 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <span className="font-semibold text-neutral-900">
                            {log.staffFullName || 'Hệ thống'}
                          </span>
                          {log.staffUsername && (
                            <span className="text-xs text-neutral-500 font-mono">
                              (@{log.staffUsername})
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Cột 3: HÀNH ĐỘNG */}
                      <td className="py-3 px-4 whitespace-nowrap">
                        {getActionBadge(log.actionType)}
                      </td>

                      {/* Cột 4: NỘI DUNG CHI TIẾT THAO TÁC (Ngắn gọn, đúng mục tiêu) */}
                      <td className="py-3 px-4 whitespace-nowrap">
                        <span className="font-medium text-neutral-900">
                          {actionDesc}
                        </span>
                      </td>

                      {/* Cột 5: TÀI KHOẢN BỊ TÁC ĐỘNG (Thay thế cột Địa chỉ IP) */}
                      <td className="py-3 px-4 whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 text-neutral-800 border border-neutral-200 text-xs font-medium">
                          <User className="w-3.5 h-3.5 text-neutral-500" />
                          <span>{targetAcc}</span>
                        </div>
                      </td>

                      {/* Cột 6: CHI TIẾT */}
                      <td className="py-3 px-4 text-right whitespace-nowrap">
                        <button
                          type="button"
                          onClick={() => openDetailModal(log)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-neutral-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Xem chi tiết dữ liệu thay đổi"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Xem Diff
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-neutral-100">
            <Pagination
              pageNumber={page}
              pageSize={pageSize}
              totalElements={totalElements}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </div>
        )}
      </div>

      {/* DETAIL DIFF MODAL — TRỰC QUAN & CÓ ĐIỂM NHẤN */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title={`Chi tiết Kiểm toán #${selectedLog?.id} — ${diffData.actionSummary}`}
        description={`Thực hiện lúc ${selectedLog?.createdAt ? new Date(selectedLog.createdAt).toLocaleString('vi-VN') : ''} bởi @${selectedLog?.staffUsername || 'System'}`}
        maxWidth="4xl"
      >
        <div className="space-y-4">
          {/* Metadata Cards: Tác nhân & Đối tượng bị tác động */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3.5 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs">
                {selectedLog?.staffFullName ? selectedLog.staffFullName.charAt(0) : 'S'}
              </div>
              <div>
                <span className="text-neutral-500 block text-[11px]">Người thực hiện:</span>
                <span className="font-semibold text-neutral-900">
                  {selectedLog?.staffFullName || 'Hệ thống'}
                </span>
                {selectedLog?.staffUsername && (
                  <span className="text-neutral-500 font-mono ml-1">(@{selectedLog.staffUsername})</span>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2.5 border-t sm:border-t-0 sm:border-l border-neutral-200 pt-2 sm:pt-0 sm:pl-3">
              <div className="w-8 h-8 rounded-full bg-red-100 text-red-700 flex items-center justify-center font-bold text-xs">
                <User className="w-4 h-4" />
              </div>
              <div>
                <span className="text-neutral-500 block text-[11px]">Tài khoản bị tác động:</span>
                <span className="font-semibold text-neutral-900">
                  {diffData.targetDisplay}
                </span>
              </div>
            </div>
          </div>

          {/* Thiết bị & IP */}
          <div className="flex flex-wrap items-center gap-3 text-xs text-neutral-500 px-1 font-mono">
            <span>
              Địa chỉ IP: <strong className="text-neutral-800">{selectedLog?.ipAddress || '—'}</strong>
            </span>
            <span>•</span>
            <span className="truncate max-w-md" title={selectedLog?.userAgent}>
              Thiết bị: <span className="text-neutral-700">{selectedLog?.userAgent || '—'}</span>
            </span>
          </div>

          {/* KHU VỰC ĐIỂM NHẤN: SO SÁNH THAY ĐỔI DỮ LIỆU */}
          <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-xs">
            <div className="bg-neutral-100/80 px-4 py-2.5 border-b border-neutral-200 flex justify-between items-center">
              <span className="text-xs font-bold text-neutral-800 uppercase tracking-wider flex items-center gap-1.5">
                <Shield className="w-4 h-4 text-red-600" />
                Đối soát Dữ liệu Thay đổi (Audit Diff)
              </span>
              {selectedLog?.actionType && getActionBadge(selectedLog.actionType)}
            </div>

            <div className="p-4 space-y-3">
              {/* Trường hợp Xóa nhiều */}
              {diffData.deletedList && (
                <div className="space-y-2">
                  <div className="text-xs font-bold text-red-700 flex items-center gap-1.5">
                    <Trash2 className="w-4 h-4" />
                    Danh sách tài khoản nhân viên đã bị xóa ({diffData.deletedList.length}):
                  </div>
                  <div className="divide-y divide-neutral-100 border border-neutral-200 rounded-lg overflow-hidden">
                    {diffData.deletedList.map((st, idx) => (
                      <div key={idx} className="p-2.5 flex items-center justify-between text-xs bg-red-50/40">
                        <span className="font-medium text-neutral-900">
                          {st.fullName} <span className="font-mono text-neutral-500">(@{st.username})</span>
                        </span>
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-red-100 text-red-800 border border-red-200">
                          Đã xóa mềm
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Danh sách trường so sánh Trước vs Sau */}
              {diffData.fields && diffData.fields.length > 0 ? (
                <div className="space-y-2.5">
                  {diffData.fields.map((f, i) => (
                    <div
                      key={i}
                      className="p-3 rounded-xl border border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs"
                    >
                      <div className="font-bold text-neutral-800 md:w-1/4">
                        {f.label}
                      </div>

                      <div className="flex-1 flex items-center gap-2.5">
                        {/* Giá trị trước thay đổi (Old Value) */}
                        <div className="flex-1">
                          {f.oldVal ? (
                            <div className="p-2 rounded-lg bg-red-50 border border-red-200 text-red-800 font-semibold line-through decoration-red-400">
                              <span className="text-[10px] text-red-500 font-normal block not-italic">Trước thay đổi:</span>
                              {f.oldVal}
                            </div>
                          ) : (
                            <div className="p-2 rounded-lg bg-neutral-100 text-neutral-400 italic">
                              (Chưa có dữ liệu)
                            </div>
                          )}
                        </div>

                        {/* Mũi tên chuyển dịch */}
                        <div className="text-neutral-400 shrink-0">
                          <ArrowRight className="w-4 h-4" />
                        </div>

                        {/* Giá trị sau thay đổi (New Value) */}
                        <div className="flex-1">
                          {f.newVal ? (
                            <div className="p-2 rounded-lg bg-emerald-50 border border-emerald-300 text-emerald-900 font-bold shadow-xs">
                              <span className="text-[10px] text-emerald-600 font-normal block">Sau thay đổi:</span>
                              {f.newVal}
                            </div>
                          ) : (
                            <div className="p-2 rounded-lg bg-neutral-100 text-neutral-400 italic">
                              (Đã bị xóa)
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : !diffData.deletedList ? (
                <div className="text-center py-6 text-neutral-400 text-xs">
                  Thao tác không có trường dữ liệu khác biệt trực tiếp. Vui lòng xem mã JSON gốc bên dưới.
                </div>
              ) : null}
            </div>
          </div>

          {/* Toggle xem JSON thô */}
          <div className="border border-neutral-200 rounded-xl overflow-hidden">
            <button
              type="button"
              onClick={() => setShowRawJson(!showRawJson)}
              className="w-full px-4 py-2.5 bg-neutral-50 hover:bg-neutral-100 flex items-center justify-between text-xs font-bold text-neutral-700 transition-colors cursor-pointer"
            >
              <div className="flex items-center gap-1.5">
                <Code2 className="w-4 h-4 text-neutral-500" />
                <span>Xem dữ liệu JSON gốc kỹ thuật (Raw Data)</span>
              </div>
              {showRawJson ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>

            {showRawJson && (
              <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-3 bg-neutral-900 border-t border-neutral-800">
                <div>
                  <div className="text-xs font-bold text-amber-400 mb-1">Dữ liệu trước (Old Values JSON):</div>
                  <pre className="p-3 bg-black/60 text-amber-300 text-[11px] font-mono rounded-lg overflow-x-auto max-h-56 leading-relaxed">
                    {formatJson(selectedLog?.oldValues)}
                  </pre>
                </div>
                <div>
                  <div className="text-xs font-bold text-emerald-400 mb-1">Dữ liệu sau (New Values JSON):</div>
                  <pre className="p-3 bg-black/60 text-emerald-300 text-[11px] font-mono rounded-lg overflow-x-auto max-h-56 leading-relaxed">
                    {formatJson(selectedLog?.newValues)}
                  </pre>
                </div>
              </div>
            )}
          </div>

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsDetailModalOpen(false)}
            >
              Đóng
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default AuditLogListPage;
