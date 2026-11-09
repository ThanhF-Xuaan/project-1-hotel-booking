import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Clock,
  ShieldCheck,
  Play,
  RotateCw,
  Search,
  CheckCircle2,
  AlertCircle,
  Cpu,
  Layers,
  Lock,
  Zap,
  Info,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import { schedulerService } from '../services/scheduler.service';
import type {
  SystemSchedulerTaskDto,
  SchedulerTriggerResultDto,
  SystemSchedulerSearchDto,
} from '../types/scheduler.types';
import { getApiErrorMessage } from '../../../core/api/error';

const CATEGORY_COLORS: Record<string, { bg: string; text: string; label: string }> = {
  INVENTORY: {
    bg: 'bg-blue-50 border-blue-200',
    text: 'text-blue-700',
    label: 'Tồn kho (Inventory)',
  },
  BOOKING: {
    bg: 'bg-purple-50 border-purple-200',
    text: 'text-purple-700',
    label: 'Đặt phòng (Booking)',
  },
  IDENTITY: {
    bg: 'bg-emerald-50 border-emerald-200',
    text: 'text-emerald-700',
    label: 'Định danh (Identity)',
  },
  OPERATION: {
    bg: 'bg-amber-50 border-amber-200',
    text: 'text-amber-800',
    label: 'Vận hành (Operation)',
  },
};

export default function SystemSchedulerListPage() {
  const [tasks, setTasks] = useState<SystemSchedulerTaskDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [keyword, setKeyword] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [triggerFilter, setTriggerFilter] = useState<'ALL' | 'MANUAL' | 'AUTO'>('ALL');

  // Trigger Action State
  const [taskToTrigger, setTaskToTrigger] = useState<SystemSchedulerTaskDto | null>(null);
  const [triggering, setTriggering] = useState<boolean>(false);
  const [triggerResult, setTriggerResult] = useState<SchedulerTriggerResultDto | null>(null);
  const [triggerError, setTriggerError] = useState<string | null>(null);

  const fetchTasks = useCallback(async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    setError(null);
    try {
      const searchDto: SystemSchedulerSearchDto = {};
      if (keyword.trim()) searchDto.keyword = keyword.trim();
      if (selectedCategory) searchDto.category = selectedCategory;
      if (triggerFilter === 'MANUAL') searchDto.allowManualTrigger = true;
      if (triggerFilter === 'AUTO') searchDto.allowManualTrigger = false;

      const data = await schedulerService.getSchedulers(searchDto);
      setTasks(data);
    } catch (err) {
      const { message } = getApiErrorMessage(
        err,
        'Không thể tải danh sách tác vụ lập lịch hệ thống'
      );
      setError(message);
    } finally {
      if (!isSilent) setLoading(false);
    }
  }, [keyword, selectedCategory, triggerFilter]);

  useEffect(() => {
    let isMounted = true;
    void (async () => {
      if (isMounted) {
        await fetchTasks();
      }
    })();
    return () => {
      isMounted = false;
    };
  }, [fetchTasks]);

  const handleResetFilters = () => {
    setKeyword('');
    setSelectedCategory('');
    setTriggerFilter('ALL');
  };

  const handleConfirmTrigger = async () => {
    if (!taskToTrigger) return;
    setTriggering(true);
    setTriggerError(null);
    setTriggerResult(null);

    try {
      const result = await schedulerService.triggerScheduler(taskToTrigger.taskKey);
      setTriggerResult(result);
      setTaskToTrigger(null);
      // Refresh task list silently to update lock statuses
      await fetchTasks(true);
    } catch (err) {
      const { message } = getApiErrorMessage(err, 'Lỗi khi kích hoạt tác vụ thủ công');
      setTriggerError(message);
    } finally {
      setTriggering(false);
    }
  };

  // Stats calculation
  const totalTasks = tasks.length;
  const shedLockProtectedCount = useMemo(
    () => tasks.filter((t) => t.hasDistributedLock).length,
    [tasks]
  );
  const manualAllowedCount = useMemo(
    () => tasks.filter((t) => t.allowManualTrigger).length,
    [tasks]
  );
  const autoOnlyCount = useMemo(
    () => tasks.filter((t) => !t.allowManualTrigger).length,
    [tasks]
  );

  return (
    <div className="space-y-6 pb-12">
      {/* ── HEADER ────────────────────────────────────────────────────────── */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-bold text-neutral-900">
                Quản Lý Tác Vụ Lập Lịch & Nền
              </h1>
              <p className="text-xs md:text-sm text-neutral-500 mt-0.5">
                Giám sát trạng thái Distributed Lock (ShedLock) và kích hoạt tác vụ nền toàn cụm
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* ── STATS CARDS ───────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4">
        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Tổng số tác vụ</span>
            <Layers className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">{totalTasks}</span>
            <span className="text-xs text-neutral-500">tác vụ nền</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-emerald-200 bg-emerald-50/20 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-800">
              Distributed Lock (ShedLock)
            </span>
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-emerald-700">
              {totalTasks > 0 ? `${shedLockProtectedCount}/${totalTasks}` : '0/0'}
            </span>
            <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
              100% Khóa
            </span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Kích hoạt thủ công</span>
            <Play className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">{manualAllowedCount}</span>
            <span className="text-xs text-neutral-500">tác vụ đủ điều kiện</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Chỉ tự động (Sự kiện/Đóng ngày)</span>
            <Cpu className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">{autoOnlyCount}</span>
            <span className="text-xs text-neutral-500">tác vụ tự động</span>
          </div>
        </div>
      </div>

      {/* ── ALERTS (Success / Error) ───────────────────────────────────────── */}
      {triggerResult && (
        <div className="bg-emerald-50 border border-emerald-300 rounded-2xl p-4 flex items-start gap-3 animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 mt-0.5 shrink-0" />
          <div className="flex-1">
            <h4 className="text-sm font-bold text-emerald-900">
              Kích hoạt thành công: {triggerResult.taskName}
            </h4>
            <p className="text-xs text-emerald-800 mt-0.5">{triggerResult.message}</p>
            {triggerResult.processedCount !== null &&
              triggerResult.processedCount !== undefined && (
                <p className="text-xs text-emerald-700 font-medium mt-1">
                  Số lượng đối tượng xử lý:{' '}
                  <span className="font-bold">{triggerResult.processedCount}</span>
                </p>
              )}
          </div>
          <button
            onClick={() => setTriggerResult(null)}
            className="text-emerald-700 hover:text-emerald-900 text-xs font-semibold underline cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {triggerError && (
        <div className="bg-red-50 border border-red-300 rounded-2xl p-4 flex items-start gap-3 animate-in fade-in">
          <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 shrink-0" />
          <div className="flex-1">
            <h4 className="text-sm font-bold text-red-900">Kích hoạt thất bại</h4>
            <p className="text-xs text-red-800 mt-0.5">{triggerError}</p>
          </div>
          <button
            onClick={() => setTriggerError(null)}
            className="text-red-700 hover:text-red-900 text-xs font-semibold underline cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* ── HÀNG 1: SEARCH & FILTER BAR ────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
          <div className="md:col-span-5">
            <div className="relative">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <Input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="Tìm theo tên hoặc mã tác vụ (taskKey)..."
                className="pl-9 h-11 rounded-xl"
              />
            </div>
          </div>

          <div className="md:col-span-3">
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full h-11 rounded-xl border border-neutral-300 bg-white px-3 text-sm text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600"
            >
              <option value="">Tất cả phân hệ</option>
              <option value="INVENTORY">Tồn kho (INVENTORY)</option>
              <option value="BOOKING">Đặt phòng (BOOKING)</option>
              <option value="IDENTITY">Định danh (IDENTITY)</option>
              <option value="OPERATION">Vận hành (OPERATION)</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <select
              value={triggerFilter}
              onChange={(e) => setTriggerFilter(e.target.value as 'ALL' | 'MANUAL' | 'AUTO')}
              className="w-full h-11 rounded-xl border border-neutral-300 bg-white px-3 text-sm text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600"
            >
              <option value="ALL">Tất cả chế độ</option>
              <option value="MANUAL">Cho phép thủ công</option>
              <option value="AUTO">Chỉ tự động nền</option>
            </select>
          </div>

          <div className="md:col-span-2 flex items-center gap-2">
            <Button
              variant="secondary"
              onClick={handleResetFilters}
              className="h-11 px-3 rounded-xl text-neutral-600 text-xs w-full"
            >
              Đặt lại
            </Button>
            <Button
              onClick={() => void fetchTasks()}
              className="h-11 px-4 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs w-full font-medium"
            >
              Lọc
            </Button>
          </div>
        </div>
      </div>

      {/* ── HÀNG 2: ACTION TOOLBAR ─────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white rounded-2xl border border-neutral-200 p-3.5 shadow-xs">
        <div className="flex items-center gap-2 text-xs text-neutral-600">
          <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>
            Bảo vệ chống chạy trùng lặp Pods/Nodes thông qua PostgreSQL Distributed Lock (
            <code className="bg-neutral-100 text-neutral-800 px-1.5 py-0.5 rounded font-mono">
              shedlock
            </code>
            )
          </span>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void fetchTasks()}
            disabled={loading}
            className="rounded-xl h-9 text-xs flex items-center gap-1.5"
          >
            <RotateCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm Mới Danh Sách</span>
          </Button>
        </div>
      </div>

      {/* ── HÀNG 3: DATA TABLE ─────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        {loading ? (
          <div className="p-12 text-center text-sm text-neutral-500 flex flex-col items-center justify-center gap-3">
            <RotateCw className="w-6 h-6 animate-spin text-red-600" />
            <span>Đang tải danh sách tác vụ lập lịch và trạng thái khóa phân tán...</span>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-sm text-red-600 space-y-3">
            <p>{error}</p>
            <Button size="sm" variant="outline" onClick={() => void fetchTasks()}>
              Thử lại
            </Button>
          </div>
        ) : tasks.length === 0 ? (
          <div className="p-12 text-center text-sm text-neutral-500">
            Không tìm thấy tác vụ lập lịch nào phù hợp với bộ lọc hiện tại.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-50/75 text-neutral-600 text-xs font-semibold uppercase tracking-wider">
                  <th className="px-4 py-3.5">Tác Vụ & Phân Hệ</th>
                  <th className="px-4 py-3.5">Chu Kỳ Thực Thi</th>
                  <th className="px-4 py-3.5">Distributed Lock (ShedLock)</th>
                  <th className="px-4 py-3.5">Cơ Chế Kích Hoạt</th>
                  <th className="px-4 py-3.5 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {tasks.map((task) => {
                  const categoryMeta = CATEGORY_COLORS[task.category] || {
                    bg: 'bg-neutral-100',
                    text: 'text-neutral-700',
                    label: task.category,
                  };

                  return (
                    <tr
                      key={task.taskKey}
                      className="hover:bg-neutral-50/70 transition-colors"
                    >
                      {/* Cột 1: Tên tác vụ, key, phân hệ & mô tả */}
                      <td className="px-4 py-4 align-top max-w-sm">
                        <div className="flex items-start gap-2">
                          <div>
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-semibold text-neutral-900">
                                {task.taskName}
                              </span>
                              <span
                                className={`text-[11px] font-medium px-2 py-0.5 rounded-full border ${categoryMeta.bg} ${categoryMeta.text}`}
                              >
                                {categoryMeta.label}
                              </span>
                            </div>
                            <div className="text-[11px] font-mono text-neutral-400 mt-0.5">
                              {task.taskKey}
                            </div>
                            {task.description && (
                              <p className="text-xs text-neutral-500 mt-1.5 line-clamp-2 leading-relaxed">
                                {task.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Cột 2: Chu kỳ thực thi */}
                      <td className="px-4 py-4 align-top whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 text-neutral-800 text-xs font-medium">
                          <Clock className="w-3.5 h-3.5 text-neutral-500" />
                          <span>{task.cronOrInterval}</span>
                        </div>
                      </td>

                      {/* Cột 3: Distributed Lock Key & Status */}
                      <td className="px-4 py-4 align-top">
                        <div className="space-y-1.5">
                          <div className="flex items-center gap-1.5">
                            <Lock className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                            <code className="text-[11px] font-mono text-neutral-700 bg-neutral-100 px-1.5 py-0.5 rounded">
                              {task.shedLockName}
                            </code>
                          </div>
                          <div>
                            {task.lockStatus === 'RUNNING_LOCKED' ? (
                              <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-amber-800 bg-amber-100 px-2 py-0.5 rounded-full animate-pulse">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                                Đang giữ khóa (LOCKED)
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                                Sẵn sàng (IDLE)
                              </span>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Cột 4: Cơ chế kích hoạt & Pattern quy định */}
                      <td className="px-4 py-4 align-top max-w-xs">
                        {task.allowManualTrigger ? (
                          <div className="space-y-1">
                            <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-800 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md">
                              <Zap className="w-3 h-3 text-emerald-600" />
                              Cho phép kích hoạt thủ công
                            </span>
                            <p className="text-[11px] text-neutral-400">
                              Có thể chạy theo lịch hoặc kích hoạt ngay khi cần
                            </p>
                          </div>
                        ) : (
                          /* Renders strictly according to pattern: "... được kích hoạt tự động" */
                          <div className="p-2 rounded-xl bg-neutral-50 border border-neutral-200 text-neutral-700">
                            <div className="flex items-start gap-1.5">
                              <Info className="w-3.5 h-3.5 text-neutral-400 mt-0.5 shrink-0" />
                              <span className="text-xs font-medium italic leading-snug">
                                {task.automaticPatternMessage ||
                                  'Tác vụ được kích hoạt tự động'}
                              </span>
                            </div>
                          </div>
                        )}
                      </td>

                      {/* Cột 5: Thao tác (Kích hoạt ngay hoặc Nhãn bảo vệ) */}
                      <td className="px-4 py-4 align-top text-right whitespace-nowrap">
                        {task.allowManualTrigger ? (
                          <Button
                            size="sm"
                            onClick={() => setTaskToTrigger(task)}
                            disabled={triggering}
                            className="rounded-xl h-9 px-3.5 bg-red-600 hover:bg-red-700 text-white font-medium text-xs shadow-xs inline-flex items-center gap-1.5"
                          >
                            <Play className="w-3.5 h-3.5 fill-current" />
                            <span>Kích Hoạt Ngay</span>
                          </Button>
                        ) : (
                          <div className="inline-flex items-center gap-1 text-[11px] text-neutral-400 italic bg-neutral-100/80 px-2.5 py-1 rounded-lg">
                            <Lock className="w-3 h-3" />
                            <span>Tự động nền</span>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ── MODAL XÁC NHẬN KÍCH HOẠT THỦ CÔNG ───────────────────────────────── */}
      {taskToTrigger && (
        <ConfirmModal
          isOpen={Boolean(taskToTrigger)}
          onClose={() => setTaskToTrigger(null)}
          onConfirm={handleConfirmTrigger}
          isLoading={triggering}
          variant="danger"
          title={`Xác nhận kích hoạt tác vụ`}
          confirmText="Xác nhận Kích hoạt"
          cancelText="Hủy bỏ"
        >
          <div className="space-y-3 text-sm text-neutral-600">
            <p>
              Bạn đang yêu cầu kích hoạt thủ công tác vụ:{' '}
              <strong className="text-neutral-900">{taskToTrigger.taskName}</strong>.
            </p>
            <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl space-y-1.5 text-xs">
              <div>
                <span className="text-neutral-500">Mã tác vụ:</span>{' '}
                <code className="font-mono font-semibold text-neutral-800">
                  {taskToTrigger.taskKey}
                </code>
              </div>
              <div>
                <span className="text-neutral-500">Khóa phân tán (ShedLock):</span>{' '}
                <code className="font-mono text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded">
                  {taskToTrigger.shedLockName}
                </code>
              </div>
              {taskToTrigger.description && (
                <div className="pt-1 text-neutral-600 border-t border-neutral-200">
                  {taskToTrigger.description}
                </div>
              )}
            </div>
            <p className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-xl border border-amber-200">
              ⚠️ Tác vụ sẽ thực thi ngay lập tức và được bảo vệ bởi cơ chế Distributed Lock. Nếu
              một Pod/Node khác đang chạy tác vụ này, hệ thống sẽ bỏ qua để chống chạy trùng lặp.
            </p>
          </div>
        </ConfirmModal>
      )}
    </div>
  );
}

