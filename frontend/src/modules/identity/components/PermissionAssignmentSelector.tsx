import React, { useState, useMemo } from 'react';
import { Search, CheckSquare, Square, Shield, Filter, Check } from 'lucide-react';
import type { PermissionDto } from '../types/role.types';

interface PermissionAssignmentSelectorProps {
  availablePermissions: PermissionDto[];
  selectedPermissionIds: number[];
  onChange: (selectedIds: number[]) => void;
}

export const PermissionAssignmentSelector: React.FC<PermissionAssignmentSelectorProps> = ({
  availablePermissions,
  selectedPermissionIds,
  onChange,
}) => {
  const [activeResource, setActiveResource] = useState<string>('ALL');
  const [searchKeyword, setSearchKeyword] = useState<string>('');

  // Gom nhóm quyền hạn theo resource
  const groupedPermissions = useMemo(() => {
    const groups: Record<string, PermissionDto[]> = {};
    availablePermissions.forEach((p) => {
      const resName = p.resource || 'CHUNG';
      if (!groups[resName]) groups[resName] = [];
      groups[resName].push(p);
    });
    return groups;
  }, [availablePermissions]);

  const resourceList = useMemo(() => {
    return Object.keys(groupedPermissions).sort();
  }, [groupedPermissions]);

  // Thiết lập nhóm active mặc định khi chưa có
  const currentActiveResource = useMemo(() => {
    if (activeResource === 'ALL') return 'ALL';
    if (!resourceList.includes(activeResource) && resourceList.length > 0) {
      return resourceList[0];
    }
    return activeResource;
  }, [activeResource, resourceList]);

  // Lọc quyền theo từ khóa tìm kiếm và nhóm active
  const displayedPermissions = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase();
    const list = currentActiveResource === 'ALL'
      ? availablePermissions
      : (groupedPermissions[currentActiveResource] || []);

    if (!kw) return list;

    return list.filter((p) => {
      const act = p.action.toLowerCase();
      const res = p.resource.toLowerCase();
      const desc = (p.description || '').toLowerCase();
      return act.includes(kw) || res.includes(kw) || desc.includes(kw);
    });
  }, [availablePermissions, groupedPermissions, currentActiveResource, searchKeyword]);

  // Toggle 1 quyền
  const togglePermission = (id: number) => {
    if (selectedPermissionIds.includes(id)) {
      onChange(selectedPermissionIds.filter((pId) => pId !== id));
    } else {
      onChange([...selectedPermissionIds, id]);
    }
  };

  // Toggle toàn bộ nhóm hiện tại
  const toggleCurrentGroup = () => {
    const targetPerms = currentActiveResource === 'ALL'
      ? availablePermissions
      : (groupedPermissions[currentActiveResource] || []);
    const targetIds = targetPerms.map((p) => p.id);
    const allSelected = targetIds.length > 0 && targetIds.every((id) => selectedPermissionIds.includes(id));

    if (allSelected) {
      onChange(selectedPermissionIds.filter((id) => !targetIds.includes(id)));
    } else {
      const newIds = new Set([...selectedPermissionIds, ...targetIds]);
      onChange(Array.from(newIds));
    }
  };

  // Toggle tất cả quyền hệ thống
  const toggleAllGlobal = () => {
    if (selectedPermissionIds.length === availablePermissions.length) {
      onChange([]);
    } else {
      onChange(availablePermissions.map((p) => p.id));
    }
  };

  // Xác định màu sắc của Action Badge
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

  const isCurrentGroupAllSelected = useMemo(() => {
    const targetPerms = currentActiveResource === 'ALL'
      ? availablePermissions
      : (groupedPermissions[currentActiveResource] || []);
    return targetPerms.length > 0 && targetPerms.every((p) => selectedPermissionIds.includes(p.id));
  }, [currentActiveResource, availablePermissions, groupedPermissions, selectedPermissionIds]);

  return (
    <div className="border border-neutral-200 rounded-2xl bg-white overflow-hidden shadow-xs">
      {/* Top Bar: Search & Global Controls */}
      <div className="p-3 bg-neutral-50/70 border-b border-neutral-200 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo quyền, module, mô tả..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-neutral-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500 transition-all placeholder:text-neutral-400"
          />
        </div>

        <div className="flex items-center justify-between sm:justify-end gap-3 w-full sm:w-auto">
          <span className="text-xs font-semibold text-neutral-600">
            Đã chọn: <span className="font-bold text-red-600">{selectedPermissionIds.length}</span> / {availablePermissions.length} quyền
          </span>
          <button
            type="button"
            onClick={toggleAllGlobal}
            className="text-xs font-bold text-red-600 hover:text-red-700 hover:underline cursor-pointer flex items-center gap-1"
          >
            {selectedPermissionIds.length === availablePermissions.length ? (
              <>
                <Square className="w-3.5 h-3.5" /> Bỏ chọn tất cả
              </>
            ) : (
              <>
                <CheckSquare className="w-3.5 h-3.5" /> Chọn tất cả quyền
              </>
            )}
          </button>
        </div>
      </div>

      {/* Master-Detail Split Pane (Fixed Height h-[360px]) */}
      <div className="flex flex-col md:flex-row h-[360px]">
        {/* CỘT TRÁI: Danh sách Nhóm Module (35% width) */}
        <div className="w-full md:w-5/12 border-b md:border-b-0 md:border-r border-neutral-200 bg-neutral-50/40 overflow-y-auto scrollbar-thin">
          <div className="p-2 space-y-1">
            {/* Nhóm TẤT CẢ */}
            <button
              type="button"
              onClick={() => setActiveResource('ALL')}
              className={`w-full text-left px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-between cursor-pointer ${
                currentActiveResource === 'ALL'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'text-neutral-700 hover:bg-neutral-100'
              }`}
            >
              <span className="flex items-center gap-1.5 truncate">
                <Filter className="w-3.5 h-3.5 shrink-0" />
                TẤT CẢ MODULE
              </span>
              <span
                className={`text-[10px] px-1.5 py-0.5 rounded-full font-bold ${
                  currentActiveResource === 'ALL'
                    ? 'bg-white/20 text-white'
                    : 'bg-neutral-200/80 text-neutral-700'
                }`}
              >
                {selectedPermissionIds.length}/{availablePermissions.length}
              </span>
            </button>

            {/* Danh sách từng Module */}
            {resourceList.map((res) => {
              const perms = groupedPermissions[res] || [];
              const checkedCount = perms.filter((p) => selectedPermissionIds.includes(p.id)).length;
              const isAllChecked = perms.length > 0 && checkedCount === perms.length;
              const isSelectedTab = currentActiveResource === res;

              return (
                <button
                  key={res}
                  type="button"
                  onClick={() => setActiveResource(res)}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs transition-all flex items-center justify-between cursor-pointer ${
                    isSelectedTab
                      ? 'bg-red-600 text-white font-bold shadow-xs'
                      : 'text-neutral-700 hover:bg-neutral-100'
                  }`}
                >
                  <span className="flex items-center gap-1.5 truncate font-semibold">
                    <Shield className="w-3.5 h-3.5 shrink-0 opacity-70" />
                    <span className="truncate">{res}</span>
                  </span>
                  <span
                    className={`text-[10px] px-1.5 py-0.5 rounded-full font-bold shrink-0 ${
                      isSelectedTab
                        ? 'bg-white/20 text-white'
                        : isAllChecked
                        ? 'bg-green-100 text-green-700 border border-green-200'
                        : checkedCount > 0
                        ? 'bg-amber-100 text-amber-700 border border-amber-200'
                        : 'bg-neutral-200/80 text-neutral-600'
                    }`}
                  >
                    {checkedCount}/{perms.length}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* CỘT PHẢI: Danh sách Quyền chi tiết của Module đang chọn (65% width) */}
        <div className="w-full md:w-7/12 flex flex-col bg-white overflow-hidden">
          {/* Header nhóm chi tiết */}
          <div className="px-4 py-2.5 bg-neutral-50/80 border-b border-neutral-200 flex items-center justify-between shrink-0">
            <span className="text-xs font-bold text-neutral-800 uppercase tracking-wide truncate">
              {currentActiveResource === 'ALL' ? 'Tất cả quyền hạn' : `Module: ${currentActiveResource}`}
              <span className="text-neutral-500 font-normal ml-1">({displayedPermissions.length} quyền)</span>
            </span>
            {displayedPermissions.length > 0 && (
              <button
                type="button"
                onClick={toggleCurrentGroup}
                className="text-[11px] font-bold text-red-600 hover:text-red-700 hover:underline cursor-pointer shrink-0"
              >
                {isCurrentGroupAllSelected ? 'Bỏ chọn nhóm này' : 'Chọn nhóm này'}
              </button>
            )}
          </div>

          {/* List Checkbox có thanh cuộn nội bộ */}
          <div className="p-3 overflow-y-auto scrollbar-thin flex-1 space-y-2">
            {displayedPermissions.length === 0 ? (
              <div className="py-12 text-center text-xs text-neutral-400">
                Không tìm thấy quyền hạn nào khớp với từ khóa tìm kiếm.
              </div>
            ) : (
              displayedPermissions.map((p) => {
                const isChecked = selectedPermissionIds.includes(p.id);
                return (
                  <label
                    key={p.id}
                    onClick={() => togglePermission(p.id)}
                    className={`flex items-start gap-3 p-2.5 rounded-xl border transition-all cursor-pointer select-none ${
                      isChecked
                        ? 'bg-red-50/60 border-red-200 text-red-950 shadow-2xs'
                        : 'bg-white border-neutral-200 hover:border-neutral-300 hover:bg-neutral-50/50 text-neutral-700'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-md mt-0.5 shrink-0 flex items-center justify-center border transition-colors ${
                        isChecked
                          ? 'bg-red-600 border-red-600 text-white'
                          : 'border-neutral-300 bg-white'
                      }`}
                    >
                      {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span
                          className={`px-1.5 py-0.5 text-[10px] font-extrabold uppercase rounded border tracking-wide ${getActionBadgeColor(
                            p.action
                          )}`}
                        >
                          {p.action}
                        </span>
                        <span className="text-xs font-bold text-neutral-900 tracking-tight">
                          {p.resource}
                        </span>
                      </div>
                      {p.description && (
                        <p className="text-[11px] text-neutral-500 mt-1 line-clamp-2 leading-relaxed">
                          {p.description}
                        </p>
                      )}
                    </div>
                  </label>
                );
              })
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PermissionAssignmentSelector;
