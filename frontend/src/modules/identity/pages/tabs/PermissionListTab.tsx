import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Search, RefreshCw, Key, ShieldCheck } from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Input from '../../../../core/components/ui/Input';
import Select from '../../../../core/components/ui/Select';
import Pagination from '../../../../core/components/ui/Pagination';
import permissionService from '../../services/permission.service';
import type { PermissionDto } from '../../types/role.types';

export const PermissionListTab: React.FC = () => {
  const [permissions, setPermissions] = useState<PermissionDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [keyword, setKeyword] = useState('');
  const [selectedResource, setSelectedResource] = useState<string>('');
  const [selectedAction, setSelectedAction] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  // Danh sách các resources để đưa vào dropdown filter
  const resourceOptions = useMemo(() => {
    return [
      { value: '', label: 'Tất cả Module / Tài nguyên' },
      { value: 'BOOKING', label: 'BOOKING (Đơn đặt phòng)' },
      { value: 'STAFF', label: 'STAFF (Nhân sự & Tài khoản)' },
      { value: 'PROPERTY', label: 'PROPERTY (Khách sạn cơ sở)' },
      { value: 'INVENTORY', label: 'INVENTORY (Quỹ phòng & Tồn)' },
      { value: 'PRICING', label: 'PRICING (Bảng giá & Phụ thu)' },
      { value: 'GUEST', label: 'GUEST (Hồ sơ Khách lưu trú)' },
      { value: 'CHAIN_DASHBOARD', label: 'CHAIN_DASHBOARD (Báo cáo Chuỗi)' },
      { value: 'REGION_DASHBOARD', label: 'REGION_DASHBOARD (Báo cáo Vùng)' },
      { value: 'PROPERTY_DASHBOARD', label: 'PROPERTY_DASHBOARD (Báo cáo Khách sạn)' },
      { value: 'SERVICE_ORDER', label: 'SERVICE_ORDER (Dịch vụ phòng)' },
      { value: 'ROOM_STATUS', label: 'ROOM_STATUS (Trạng thái phòng)' },
      { value: 'MAINTENANCE_TICKET', label: 'MAINTENANCE_TICKET (Bảo trì)' },
    ];
  }, []);

  const actionOptions = useMemo(() => {
    return [
      { value: '', label: 'Tất cả Hành động (Actions)' },
      { value: 'VIEW', label: 'VIEW (Xem / Tra cứu)' },
      { value: 'CREATE', label: 'CREATE (Thêm mới)' },
      { value: 'UPDATE', label: 'UPDATE (Chỉnh sửa / Cập nhật)' },
      { value: 'CANCEL', label: 'CANCEL (Hủy bỏ)' },
      { value: 'DELETE', label: 'DELETE (Xóa)' },
    ];
  }, []);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await permissionService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        resource: selectedResource || undefined,
        action: selectedAction || undefined,
      });

      if (response?.result) {
        setPermissions(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh mục quyền hạn:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, keyword, selectedResource, selectedAction]);

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
    setKeyword('');
    setSelectedResource('');
    setSelectedAction('');
    setPage(0);
  };

  const getActionBadgeColor = (action: string) => {
    switch (action.toUpperCase()) {
      case 'VIEW':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'CREATE':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'UPDATE':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCEL':
      case 'DELETE':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-neutral-100 text-neutral-700 border-neutral-200';
    }
  };

  return (
    <div className="space-y-4 animate-in fade-in duration-150">
      {/* STANDARD 3-ROW LAYOUT */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <form
          onSubmit={handleSearch}
          className="p-4 md:p-5 border-b border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row items-center gap-3"
        >
          <div className="flex-1 w-full">
            <Input
              placeholder="Tìm theo quyền, tài nguyên hoặc mô tả..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="w-full md:w-56">
            <Select<string>
              value={selectedResource}
              onChange={(val) => {
                setSelectedResource(val);
                setPage(0);
              }}
              options={resourceOptions}
            />
          </div>

          <div className="w-full md:w-48">
            <Select<string>
              value={selectedAction}
              onChange={(val) => {
                setSelectedAction(val);
                setPage(0);
              }}
              options={actionOptions}
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <Button
              type="submit"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 text-white font-bold h-12 px-6 rounded-xl shadow-xs"
            >
              <Search className="w-4 h-4 mr-2" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              className="h-12 px-4 rounded-xl text-neutral-600 hover:text-neutral-900 border-neutral-200"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="px-5 py-3.5 border-b border-neutral-200 bg-white flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="p-1.5 bg-neutral-100 text-neutral-700 rounded-lg">
              <Key className="w-4 h-4" />
            </span>
            <div>
              <span className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
                Danh mục đặc quyền hệ thống
              </span>
              <p className="text-[11px] text-neutral-500">
                Các quyền hạn được gắn cứng với API endpoints và cơ chế kiểm soát truy cập `@PreAuthorize`
              </p>
            </div>
          </div>

          <div className="text-xs font-semibold text-neutral-600 bg-neutral-100 px-3 py-1.5 rounded-full self-end sm:self-auto">
            Tổng cộng: <span className="font-bold text-neutral-900">{totalElements}</span> quyền hạn
          </div>
        </div>

        {/* ROW 3: DATA TABLE */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50/80 border-b border-neutral-200 text-[11px] font-bold text-neutral-600 uppercase tracking-wider">
                <th className="p-3.5 pl-5">Tài nguyên (Resource)</th>
                <th className="p-3.5">Hành động</th>
                <th className="p-3.5">Mã quyền RBAC</th>
                <th className="p-3.5">Mô tả nghiệp vụ</th>
                <th className="p-3.5">Vai trò đang sở hữu</th>
                <th className="p-3.5 text-center pr-5">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 text-xs">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-neutral-400">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                      <span>Đang tải danh mục quyền hạn...</span>
                    </div>
                  </td>
                </tr>
              ) : permissions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-neutral-400">
                    Không tìm thấy quyền hạn nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                permissions.map((p) => {
                  return (
                    <tr key={p.id} className="hover:bg-neutral-50/60 transition-colors">
                      <td className="p-3.5 pl-5 font-bold text-neutral-900">
                        <span className="inline-flex items-center gap-1.5">
                          <ShieldCheck className="w-4 h-4 text-neutral-400 shrink-0" />
                          {p.resource}
                        </span>
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`px-2 py-0.5 text-[10px] font-extrabold uppercase rounded border tracking-wide ${getActionBadgeColor(
                            p.action
                          )}`}
                        >
                          {p.action}
                        </span>
                      </td>
                      <td className="p-3.5 font-mono text-[11px] text-neutral-700 font-semibold">
                        ROLE_{p.resource}_{p.action}
                      </td>
                      <td className="p-3.5 text-neutral-600 max-w-xs">
                        {p.description || `${p.action} ${p.resource}`}
                      </td>
                      <td className="p-3.5 max-w-sm">
                        {p.roleNames && p.roleNames.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {p.roleNames.map((roleName, idx) => (
                              <span
                                key={idx}
                                className="inline-block px-2 py-0.5 bg-neutral-100 text-neutral-700 text-[10px] font-medium rounded-md border border-neutral-200"
                              >
                                {roleName}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <span className="text-neutral-400 italic text-[11px]">Chưa gán vai trò nào</span>
                        )}
                      </td>
                      <td className="p-3.5 text-center pr-5">
                        <span className="inline-block px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-green-50 text-green-700 border border-green-200">
                          Kích hoạt
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <Pagination
          pageNumber={page}
          pageSize={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
        />
      </div>
    </div>
  );
};

export default PermissionListTab;
