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
  CalendarCheck,
  CreditCard,
  Tag,
  Building2,
  Layers,
  ClipboardList,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import Modal from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import auditLogService from '../services/auditLog.service';
import type { AuditLogDto } from '../types/auditLog.types';

const FIELD_LABELS: Record<string, string> = {
  // Staff & Identity
  status: 'Trạng thái',
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

  // Pricing & Rules
  name: 'Tên quy tắc / Đối tượng',
  ruleName: 'Tên quy tắc giá',
  adjustmentType: 'Loại điều chỉnh',
  adjustmentValue: 'Giá trị điều chỉnh',
  startDate: 'Ngày bắt đầu',
  endDate: 'Ngày kết thúc',
  priority: 'Mức độ ưu tiên',
  discountType: 'Loại giảm giá',
  discountValue: 'Giá trị giảm giá',
  minBookingAmount: 'Giá trị đơn tối thiểu',
  maxDiscountAmount: 'Giảm tối đa',

  // Booking & Rooms
  bookingCode: 'Mã đơn đặt phòng',
  bookingId: 'Mã đơn đặt',
  bookingRoomId: 'Mã phòng đặt',
  fromRoomNumber: 'Từ phòng',
  toRoomNumber: 'Sang phòng',
  checkInDate: 'Ngày nhận phòng',
  checkOutDate: 'Ngày trả phòng',
  totalAmount: 'Tổng tiền',
  refundAmount: 'Số tiền hoàn lại',
  amount: 'Số tiền giao dịch',
  paymentMethod: 'Phương thức thanh toán',
  paymentStatus: 'Trạng thái thanh toán',
  transactionId: 'Mã giao dịch',

  // Operations & Legal Compliance (PDPD / ANTT / Kế toán)
  identityNumber: 'Số CCCD/Hộ chiếu',
  documentNumber: 'Số giấy tờ tùy thân',
  nationality: 'Quốc tịch',
  roomNumber: 'Số phòng lưu trú',
  hotelId: 'Mã khách sạn',
  auditDate: 'Ngày kiểm toán',
  businessDate: 'Ngày kinh doanh',
  invoiceNumber: 'Số hóa đơn GTGT',
  folioNumber: 'Mã Sổ cái Folio',
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
  const [entityName, setEntityName] = useState('');
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
        entityName: entityName || undefined,
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
  }, [page, pageSize, actionType, entityName, staffKeyword, fromDate, toDate]);

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
    setEntityName('');
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
      case 'GUEST_CREATE':
      case 'STAY_GUEST_CREATE':
      case 'WALK_IN_CREATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            CREATE
          </span>
        );
      case 'UPDATE':
      case 'STAFF_UPDATE':
      case 'PRICING_RULE_UPDATE':
      case 'GUEST_UPDATE':
      case 'STAY_GUEST_UPDATE':
      case 'CONSENT_UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
            UPDATE
          </span>
        );
      case 'DELETE':
      case 'STAFF_DELETE_BATCH':
      case 'PRICING_RULE_DELETE':
      case 'GUEST_DELETE':
      case 'STAY_GUEST_DELETE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-800 border border-red-200">
            DELETE
          </span>
        );
      case 'STATUS_CHANGED':
      case 'STATUS_UPDATE':
      case 'STAFF_STATUS_UPDATE':
      case 'FOLIO_STATUS_UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            CHANGED
          </span>
        );
      case 'CHECK_IN':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-teal-100 text-teal-800 border border-teal-200">
            CHECK_IN
          </span>
        );
      case 'CHECK_OUT':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-100 text-rose-800 border border-rose-200">
            CHECK_OUT
          </span>
        );
      case 'INVOICE_CREATE':
      case 'INVOICE_ISSUE':
      case 'PAYMENT_CREATE':
      case 'PAYMENT_COMPLETE':
      case 'PAYMENT_REFUND':
      case 'FOLIO_ADJUSTMENT':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-800 border border-purple-200">
            {action}
          </span>
        );
      case 'OCR_SCAN':
      case 'OCR_SCAN_FILE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-cyan-100 text-cyan-800 border border-cyan-200">
            OCR
          </span>
        );
      case 'NIGHT_AUDIT_EXECUTE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
            NIGHT_AUDIT
          </span>
        );
      case 'LODGING_EXPORT':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            EXPORT_BCA
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
   * Phân loại danh mục tài nguyên để gắn icon và màu sắc phù hợp
   */
  const getResourceCategory = (log: AuditLogDto): string => {
    if (log.resourceCategory) return log.resourceCategory.toUpperCase();
    const entity = (log.entityName || '').toUpperCase();
    if (entity.includes('STAFF') || entity.includes('USER') || entity.includes('AUTH')) return 'STAFF';
    if (entity.includes('GUEST') || entity.includes('OCR') || entity.includes('CONSENT')) return 'GUEST';
    if (entity.includes('STAY') || entity.includes('LODGING') || entity.includes('NIGHT')) return 'OPERATION';
    if (entity.includes('BOOKING')) return 'BOOKING';
    if (entity.includes('PAYMENT') || entity.includes('FOLIO') || entity.includes('INVOICE')) return 'FINANCE';
    if (entity.includes('PRICING') || entity.includes('RULE') || entity.includes('VOUCHER')) return 'PRICING';
    if (entity.includes('HOTEL') || entity.includes('ROOM') || entity.includes('MEDIA')) return 'ORGANIZATION';
    return 'OTHER';
  };

  /**
   * Render Badge trực quan cho Đối tượng tác động kèm Icon tương ứng
   */
  const renderResourceBadge = (log: AuditLogDto, targetDisplay: string) => {
    const category = getResourceCategory(log);
    switch (category) {
      case 'STAFF':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-50 text-blue-900 border border-blue-200 text-xs font-medium">
            <User className="w-3.5 h-3.5 text-blue-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'GUEST':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-sky-50 text-sky-900 border border-sky-200 text-xs font-medium">
            <User className="w-3.5 h-3.5 text-sky-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'OPERATION':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-teal-50 text-teal-900 border border-teal-200 text-xs font-medium">
            <ClipboardList className="w-3.5 h-3.5 text-teal-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'BOOKING':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-900 border border-emerald-200 text-xs font-medium">
            <CalendarCheck className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'FINANCE':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-purple-50 text-purple-900 border border-purple-200 text-xs font-medium">
            <CreditCard className="w-3.5 h-3.5 text-purple-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'PRICING':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-amber-50 text-amber-900 border border-amber-200 text-xs font-medium">
            <Tag className="w-3.5 h-3.5 text-amber-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      case 'ORGANIZATION':
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-rose-50 text-rose-900 border border-rose-200 text-xs font-medium">
            <Building2 className="w-3.5 h-3.5 text-rose-600 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
      default:
        return (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 text-neutral-800 border border-neutral-200 text-xs font-medium">
            <Layers className="w-3.5 h-3.5 text-neutral-500 shrink-0" />
            <span className="truncate max-w-[200px]" title={targetDisplay}>{targetDisplay}</span>
          </div>
        );
    }
  };

  /**
   * Định dạng Nội dung thao tác ngắn gọn, tôn trọng mô tả từ backend
   */
  const formatActionDescription = (log: AuditLogDto) => {
    // 1. Ưu tiên số 1: Tôn trọng mô tả chi tiết được truyền từ Backend (@Auditable.description)
    if (log.description && log.description.trim().length > 0) {
      return log.description;
    }

    // 2. Fallback động theo cặp (entityName, actionType)
    const act = log.actionType?.toUpperCase() || '';
    const entity = (log.entityName || '').toUpperCase();

    if (act.includes('CHECK_IN')) return 'Check-in nhận phòng lưu trú';
    if (act.includes('CHECK_OUT')) return 'Check-out trả phòng và đóng sổ';
    if (act.includes('INVOICE_ISSUE')) return 'Phát hành hóa đơn GTGT điện tử';
    if (act.includes('INVOICE_CREATE')) return 'Tạo mới hóa đơn VAT';
    if (act.includes('NIGHT_AUDIT')) return 'Chốt sổ kiểm toán đêm';
    if (act.includes('OCR')) return 'Quét OCR trích xuất giấy tờ tùy thân';
    if (act.includes('LODGING_EXPORT')) return 'Xuất file báo cáo lưu trú gửi BCA';
    if (act.includes('FOLIO')) return 'Điều chỉnh công nợ Sổ cái Folio';
    if (act.includes('PAYMENT_REFUND')) return 'Hoàn tiền giao dịch';
    if (act.includes('PAYMENT')) return 'Giao dịch thanh toán';

    if (act === 'LOGIN' || act === 'STAFF_LOGIN') {
      return 'Đăng nhập hệ thống';
    }
    if (act === 'PASSWORD_RESET' || act === 'STAFF_PASSWORD_RESET' || act === 'RESET_PASS') {
      return 'Đặt lại mật khẩu tài khoản';
    }
    if (act.includes('STATUS')) {
      if (log.newValues?.includes('SUSPENDED')) return 'Khóa trạng thái (SUSPENDED)';
      if (log.newValues?.includes('ACTIVE')) return 'Kích hoạt trạng thái (ACTIVE)';
      return `Cập nhật trạng thái ${log.entityName || ''}`;
    }

    if (act.includes('CREATE')) {
      if (entity.includes('STAFF')) return 'Tạo mới tài khoản nhân sự';
      if (entity.includes('PRICING')) return 'Tạo mới quy tắc giá';
      if (entity.includes('BOOKING')) return 'Tạo mới đơn đặt phòng';
      if (entity.includes('HOTEL')) return 'Tạo mới khách sạn cơ sở';
      return `Tạo mới ${log.entityName || 'đối tượng'}`;
    }
    if (act.includes('UPDATE')) {
      if (entity.includes('STAFF')) return 'Cập nhật thông tin nhân sự';
      if (entity.includes('PRICING')) return 'Cập nhật quy tắc giá';
      if (entity.includes('BOOKING')) return 'Cập nhật đơn đặt phòng';
      if (entity.includes('HOTEL')) return 'Cập nhật thông tin khách sạn';
      return `Cập nhật ${log.entityName || 'đối tượng'}`;
    }
    if (act.includes('DELETE')) {
      if (entity.includes('STAFF')) return 'Xóa tài khoản nhân sự';
      if (entity.includes('PRICING')) return 'Xóa quy tắc giá';
      if (entity.includes('BOOKING')) return 'Hủy/Xóa đơn đặt phòng';
      return `Xóa ${log.entityName || 'đối tượng'}`;
    }

    return log.actionType;
  };

  const safeParseJson = (jsonStr?: string): Record<string, unknown> | null => {
    if (!jsonStr) return null;
    try {
      const parsed: unknown = JSON.parse(jsonStr);
      if (parsed && typeof parsed === 'object') {
        return parsed as Record<string, unknown>;
      }
    } catch {
      return null;
    }
    return null;
  };

  /**
   * Trích xuất Đối tượng tác động (Target Resource) linh hoạt cho mọi thực thể
   */
  const formatTargetResource = (log: AuditLogDto): string => {
    if (log.targetResource && log.targetResource.trim().length > 0) {
      return log.targetResource;
    }
    if (log.targetAccount && log.targetAccount.trim().length > 0) {
      return log.targetAccount;
    }

    const newObj = safeParseJson(log.newValues);
    if (newObj) {
      if (typeof newObj.targetResource === 'string') return newObj.targetResource;
      if (typeof newObj.targetAccount === 'string') return newObj.targetAccount;
      if (
        newObj.request &&
        typeof newObj.request === 'object' &&
        'username' in newObj.request &&
        typeof (newObj.request as { username?: unknown }).username === 'string'
      ) {
        return `@${(newObj.request as { username: string }).username}`;
      }
      if (
        newObj.request &&
        typeof newObj.request === 'object' &&
        'name' in newObj.request &&
        typeof (newObj.request as { name?: unknown }).name === 'string'
      ) {
        return `${(newObj.request as { name: string }).name} (#${log.entityId})`;
      }
      if (
        newObj.request &&
        typeof newObj.request === 'object' &&
        'bookingCode' in newObj.request &&
        typeof (newObj.request as { bookingCode?: unknown }).bookingCode === 'string'
      ) {
        return `Đơn ${(newObj.request as { bookingCode: string }).bookingCode}`;
      }
    }

    const oldObj = safeParseJson(log.oldValues);
    if (oldObj) {
      if (typeof oldObj.targetResource === 'string') return oldObj.targetResource;
      if (typeof oldObj.targetAccount === 'string') return oldObj.targetAccount;
      if (
        typeof oldObj.fullName === 'string' &&
        typeof oldObj.username === 'string'
      ) {
        return `${oldObj.fullName} (@${oldObj.username})`;
      }
      if (typeof oldObj.username === 'string') return `@${oldObj.username}`;
      if (typeof oldObj.name === 'string') return `${oldObj.name} (#${log.entityId})`;
    }

    if (log.entityName === 'Staff') {
      if (log.entityId === 'admin') return 'System Admin (@admin)';
      return `Nhân sự #${log.entityId}`;
    }

    return log.entityId ? `${log.entityName} #${log.entityId}` : (log.entityName || '—');
  };

  /**
   * Phân tích Diff giữa oldValues và newValues
   */
  const parseDiff = (log: AuditLogDto | null) => {
    if (!log) {
      return { fields: [], targetDisplay: 'N/A', actionSummary: '', deletedList: null };
    }

    const oldObj: Record<string, unknown> = safeParseJson(log.oldValues) || {};
    let newObj: Record<string, unknown> = {};

    const parsedNew = safeParseJson(log.newValues);
    if (parsedNew) {
      if (
        parsedNew.request &&
        typeof parsedNew.request === 'object' &&
        !Array.isArray(parsedNew.request)
      ) {
        newObj = { ...(parsedNew.request as Record<string, unknown>), ...parsedNew };
      } else if (
        parsedNew.params &&
        typeof parsedNew.params === 'object'
      ) {
        newObj = { ...(parsedNew.params as Record<string, unknown>), ...parsedNew };
      } else {
        newObj = parsedNew;
      }
    }

    const targetDisplay = formatTargetResource(log);
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

    // Xử lý CREATE (khi chưa có oldValues) cho mọi thực thể
    if (log.actionType.toUpperCase().includes('CREATE')) {
      const createFields: DiffField[] = [];
      const ignoredCreateKeys = new Set([
        'targetAccount',
        'targetResource',
        'actionDescription',
        'request',
        'params',
        'response',
        'deletedStaffs',
        'password',
        'confirmPassword',
        'id',
      ]);

      for (const [k, v] of Object.entries(newObj)) {
        if (ignoredCreateKeys.has(k)) continue;
        if (v !== undefined && v !== null && typeof v !== 'object') {
          createFields.push({
            label: FIELD_LABELS[k] || k,
            key: k,
            oldVal: null,
            newVal: String(v),
            status: 'added',
          });
        }
      }
      if (createFields.length > 0) {
        return {
          fields: createFields,
          targetDisplay,
          actionSummary,
          deletedList: null,
        };
      }
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
          <div className="lg:col-span-2">
            <Select<string>
              value={entityName}
              onChange={(val) => setEntityName(val)}
              options={[
                { value: '', label: 'Tất cả phân hệ' },
                { value: 'Staff', label: 'Nhân sự & Tài khoản' },
                { value: 'Booking', label: 'Đơn đặt phòng' },
                { value: 'BookingRoom', label: 'Phòng đặt' },
                { value: 'StayGuest', label: 'Khách lưu trú phòng' },
                { value: 'Guest', label: 'Hồ sơ khách hàng' },
                { value: 'Payment', label: 'Thanh toán & Giao dịch' },
                { value: 'Invoice', label: 'Hóa đơn GTGT' },
                { value: 'MasterFolio', label: 'Sổ cái Master Folio' },
                { value: 'NightAudit', label: 'Kiểm toán đêm' },
                { value: 'LodgingQueue', label: 'Khai báo tạm trú BCA' },
                { value: 'PricingRule', label: 'Quy tắc giá & Ưu đãi' },
                { value: 'Hotel', label: 'Khách sạn cơ sở' },
              ]}
              size="lg"
            />
          </div>

          <div className="lg:col-span-2">
            <Select<string>
              value={actionType}
              onChange={(val) => setActionType(val)}
              options={[
                { value: '', label: 'Tất cả hành động' },
                { value: 'CREATE', label: 'Tạo mới (CREATE)' },
                { value: 'UPDATE', label: 'Cập nhật (UPDATE)' },
                { value: 'DELETE', label: 'Xóa (DELETE)' },
                { value: 'STATUS_CHANGED', label: 'Đổi trạng thái (CHANGED)' },
                { value: 'CHECK_IN', label: 'Nhận phòng (Check-in)' },
                { value: 'CHECK_OUT', label: 'Trả phòng (Check-out)' },
                { value: 'INVOICE_ISSUE', label: 'Phát hành HĐ (Invoice)' },
                { value: 'NIGHT_AUDIT_EXECUTE', label: 'Chốt kiểm toán đêm' },
                { value: 'OCR_SCAN', label: 'Quét OCR giấy tờ' },
                { value: 'LODGING_EXPORT', label: 'Xuất file tạm trú BCA' },
                { value: 'PASSWORD_RESET', label: 'Đặt lại mật khẩu' },
              ]}
              size="lg"
            />
          </div>

          <div className="lg:col-span-2">
            <Input
              placeholder="Nhân sự thực hiện (@user...)"
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
          <ShieldAlert className="w-4 h-4 text-emerald-600" />
          <span>Hệ thống lưu trữ vết kiểm toán tuân thủ Nghị định 13/2023/NĐ-CP (PDPD) và Luật An ninh mạng 2018 (thời hạn lưu trữ tối thiểu 12 tháng). Dữ liệu được bảo vệ chống sửa đổi.</span>
        </div>
        <div>
          Tổng số: <strong className="text-neutral-900 font-mono">{totalElements}</strong> sự kiện kiểm toán
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
                <th className="py-3.5 px-4 whitespace-nowrap">Đối tượng tác động</th>
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
                  const targetResource = formatTargetResource(log);
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

                      {/* Cột 5: ĐỐI TƯỢNG TÁC ĐỘNG (Linh hoạt theo phân hệ) */}
                      <td className="py-3 px-4 whitespace-nowrap">
                        {renderResourceBadge(log, targetResource)}
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
              <div className="w-8 h-8 rounded-full bg-neutral-100 flex items-center justify-center font-bold text-xs">
                {selectedLog && getResourceCategory(selectedLog) === 'STAFF' ? (
                  <User className="w-4 h-4 text-blue-600" />
                ) : selectedLog && getResourceCategory(selectedLog) === 'BOOKING' ? (
                  <CalendarCheck className="w-4 h-4 text-emerald-600" />
                ) : selectedLog && getResourceCategory(selectedLog) === 'FINANCE' ? (
                  <CreditCard className="w-4 h-4 text-purple-600" />
                ) : selectedLog && getResourceCategory(selectedLog) === 'PRICING' ? (
                  <Tag className="w-4 h-4 text-amber-600" />
                ) : selectedLog && getResourceCategory(selectedLog) === 'ORGANIZATION' ? (
                  <Building2 className="w-4 h-4 text-rose-600" />
                ) : (
                  <Layers className="w-4 h-4 text-neutral-600" />
                )}
              </div>
              <div>
                <span className="text-neutral-500 block text-[11px]">Đối tượng tác động:</span>
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
