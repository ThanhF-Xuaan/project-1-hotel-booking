import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Image as ImageIcon,
  Upload,
  Search,
  RotateCw,
  LayoutGrid,
  Table as TableIcon,
  Star,
  Trash2,
  Layers,
  HardDrive,
  Building2,
  BedDouble,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Pagination from '../../../core/components/ui/Pagination';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import { mediaService } from '../services/media.service';
import hotelService from '../../organization/services/hotel.service';
import MediaUploadModal from '../components/MediaUploadModal';
import MediaDetailModal from '../components/MediaDetailModal';
import type {
  CategoryMetaDto,
  MediaItemDto,
  MediaSearchDto,
  StorageCategory,
} from '../types/media.types';

export default function MediaManagementPage() {
  const [mediaList, setMediaList] = useState<MediaItemDto[]>([]);
  const [categories, setCategories] = useState<CategoryMetaDto[]>([]);
  const [hotels, setHotels] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Pagination & Filtering
  const [keyword, setKeyword] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<StorageCategory | ''>('');
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('');
  const [primaryFilter, setPrimaryFilter] = useState<'ALL' | 'PRIMARY' | 'NORMAL'>('ALL');
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(12);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // View Mode
  const [viewMode, setViewMode] = useState<'grid' | 'table'>('grid');

  // Modals
  const [uploadModalOpen, setUploadModalOpen] = useState<boolean>(false);
  const [detailMedia, setDetailMedia] = useState<MediaItemDto | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  // Load Categories & Hotels on mount
  useEffect(() => {
    let isMounted = true;
    Promise.all([
      mediaService.getCategories(),
      hotelService.filter({ page: 0, pageSize: 100 }),
    ])
      .then(([cats, hotelRes]) => {
        if (!isMounted) return;
        setCategories(cats);
        if (hotelRes?.result?.content) {
          setHotels(hotelRes.result.content.map((h) => ({ id: h.id, name: h.name })));
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải metadata danh mục & khách sạn:', err);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const loadMedia = useCallback(async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    setErrorMsg(null);
    try {
      const searchDto: MediaSearchDto = {
        page,
        pageSize,
        sortBy: 'createdAt',
        sortDirection: 'desc',
      };
      if (keyword.trim()) searchDto.keyword = keyword.trim();
      if (selectedCategory) searchDto.category = selectedCategory;
      if (selectedHotelId) searchDto.hotelId = Number(selectedHotelId);
      if (primaryFilter === 'PRIMARY') searchDto.isPrimary = true;
      if (primaryFilter === 'NORMAL') searchDto.isPrimary = false;

      const res = await mediaService.filter(searchDto);
      setMediaList(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      console.error('Lỗi khi tải kho media:', err);
      setErrorMsg('Không thể tải danh sách tệp đa phương tiện');
    } finally {
      if (!isSilent) setLoading(false);
    }
  }, [keyword, selectedCategory, selectedHotelId, primaryFilter, page, pageSize]);

  useEffect(() => {
    let isMounted = true;
    void (async () => {
      if (isMounted) {
        await loadMedia();
      }
    })();
    return () => {
      isMounted = false;
    };
  }, [loadMedia]);

  const handleResetFilters = () => {
    setKeyword('');
    setSelectedCategory('');
    setSelectedHotelId('');
    setPrimaryFilter('ALL');
    setPage(0);
  };

  const handleSetPrimary = async (id: number) => {
    try {
      await mediaService.setPrimary(id);
      await loadMedia(true);
    } catch (err) {
      console.error('Lỗi khi đặt ảnh chính:', err);
    }
  };

  const handleDeleteMedia = async () => {
    if (!deletingId) return;
    setIsDeleting(true);
    try {
      await mediaService.delete(deletingId);
      setDeletingId(null);
      await loadMedia(true);
    } catch (err) {
      console.error('Lỗi khi xóa media:', err);
    } finally {
      setIsDeleting(false);
    }
  };

  // Stats calculation
  const totalSizeBytes = useMemo(
    () => mediaList.reduce((acc, curr) => acc + (curr.sizeBytes || 0), 0),
    [mediaList]
  );

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  return (
    <div className="space-y-6 pb-12">
      {/* ── HEADER ────────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2.5 bg-red-50 text-red-600 rounded-xl">
            <ImageIcon className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-neutral-900 tracking-tight">
              Kho Đa Phương Tiện Tập Trung
            </h1>
            <p className="text-xs md:text-sm text-neutral-500 mt-0.5">
              Quản lý toàn diện hình ảnh cơ sở, hạng phòng, tiện ích và banner thương hiệu toàn chuỗi
            </p>
          </div>
        </div>
      </div>

      {/* ── STATS CARDS ───────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4">
        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Tổng số tệp ảnh</span>
            <Layers className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">{totalElements}</span>
            <span className="text-xs text-neutral-500">hình ảnh</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Dung lượng trang hiện tại</span>
            <HardDrive className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">
              {formatBytes(totalSizeBytes)}
            </span>
            <span className="text-xs text-neutral-500">lưu trữ</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Ảnh khuôn viên khách sạn</span>
            <Building2 className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">
              {mediaList.filter((m) => m.category === 'HOTEL_PROPERTY_PHOTO').length}
            </span>
            <span className="text-xs text-neutral-500">trên trang này</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-neutral-500">Ảnh hạng phòng cơ sở</span>
            <BedDouble className="w-4 h-4 text-neutral-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-neutral-900">
              {mediaList.filter((m) => m.category === 'HOTEL_ROOM_PHOTO').length}
            </span>
            <span className="text-xs text-neutral-500">trên trang này</span>
          </div>
        </div>
      </div>

      {/* ── HÀNG 1: SEARCH & FILTER BAR ────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
          <div className="md:col-span-4">
            <div className="relative">
              <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <Input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="Tìm theo tên tệp gốc..."
                className="pl-9 h-11 rounded-xl"
              />
            </div>
          </div>

          <div className="md:col-span-3">
            <select
              value={selectedCategory}
              onChange={(e) => {
                setSelectedCategory(e.target.value as StorageCategory | '');
                setPage(0);
              }}
              className="w-full h-11 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả danh mục ảnh</option>
              {categories.map((c) => (
                <option key={c.category} value={c.category}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>

          <div className="md:col-span-3">
            <select
              value={selectedHotelId}
              onChange={(e) => {
                setSelectedHotelId(e.target.value ? Number(e.target.value) : '');
                setPage(0);
              }}
              className="w-full h-11 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả khách sạn</option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
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
              onClick={() => void loadMedia()}
              className="h-11 px-4 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs w-full font-semibold"
            >
              Lọc
            </Button>
          </div>
        </div>
      </div>

      {/* ── HÀNG 2: ACTION TOOLBAR ─────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white rounded-2xl border border-neutral-200 p-3.5 shadow-xs">
        <div className="flex items-center gap-2">
          {/* View mode toggle */}
          <div className="inline-flex rounded-xl bg-neutral-100 p-1">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors ${
                viewMode === 'grid'
                  ? 'bg-white text-neutral-900 shadow-2xs'
                  : 'text-neutral-500 hover:text-neutral-900'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>Lưới ảnh</span>
            </button>
            <button
              onClick={() => setViewMode('table')}
              className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors ${
                viewMode === 'table'
                  ? 'bg-white text-neutral-900 shadow-2xs'
                  : 'text-neutral-500 hover:text-neutral-900'
              }`}
            >
              <TableIcon className="w-3.5 h-3.5" />
              <span>Bảng chi tiết</span>
            </button>
          </div>

          <span className="text-xs text-neutral-500 hidden sm:inline">
            Hiển thị {mediaList.length} / {totalElements} tệp
          </span>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void loadMedia()}
            disabled={loading}
            className="rounded-xl h-9 text-xs flex items-center gap-1.5"
          >
            <RotateCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm Mới</span>
          </Button>

          <Button
            size="sm"
            onClick={() => setUploadModalOpen(true)}
            className="rounded-xl h-9 px-3.5 bg-red-600 hover:bg-red-700 text-white font-semibold text-xs shadow-xs flex items-center gap-1.5"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>+ Tải Lên Ảnh Mới</span>
          </Button>
        </div>
      </div>

      {/* ── HÀNG 3: DATA VIEW (GRID OR TABLE) & PAGINATION ─────────────────── */}
      {loading ? (
        <div className="p-16 bg-white rounded-2xl border border-neutral-200 text-center flex flex-col items-center justify-center gap-3">
          <RotateCw className="w-6 h-6 animate-spin text-red-600" />
          <span className="text-xs text-neutral-500 font-medium">
            Đang tải dữ liệu kho đa phương tiện...
          </span>
        </div>
      ) : errorMsg ? (
        <div className="p-8 bg-white rounded-2xl border border-red-200 text-center space-y-3">
          <p className="text-sm text-red-600 font-medium">{errorMsg}</p>
          <Button size="sm" variant="outline" onClick={() => void loadMedia()}>
            Thử lại
          </Button>
        </div>
      ) : mediaList.length === 0 ? (
        <div className="p-16 bg-white rounded-2xl border border-neutral-200 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-neutral-100 text-neutral-400 flex items-center justify-center mx-auto">
            <ImageIcon className="w-6 h-6" />
          </div>
          <p className="text-sm font-semibold text-neutral-800">
            Không tìm thấy hình ảnh nào phù hợp với bộ lọc.
          </p>
          <Button size="sm" onClick={() => setUploadModalOpen(true)} className="rounded-xl bg-red-600 text-white text-xs">
            + Tải lên tệp ảnh đầu tiên
          </Button>
        </div>
      ) : viewMode === 'grid' ? (
        /* ── GRID VIEW ── */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
          {mediaList.map((media) => (
            <div
              key={media.id}
              onClick={() => setDetailMedia(media)}
              className="group relative bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-2xs hover:shadow-md hover:border-red-300 transition-all cursor-pointer flex flex-col"
            >
              {/* Thumbnail */}
              <div className="relative aspect-4/3 w-full bg-neutral-100 overflow-hidden">
                <img
                  src={media.url}
                  alt={media.originalFilename}
                  loading="lazy"
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                />
                {media.isPrimary && (
                  <span className="absolute top-2 left-2 px-2 py-0.5 bg-amber-500 text-white rounded-full text-[10px] font-bold flex items-center gap-1 shadow-sm">
                    <Star className="w-3 h-3 fill-current" />
                    Chính
                  </span>
                )}
                <span className="absolute bottom-2 right-2 px-1.5 py-0.5 bg-black/60 text-white rounded text-[10px] font-mono">
                  {formatBytes(media.sizeBytes)}
                </span>
              </div>

              {/* Info */}
              <div className="p-2.5 flex-1 flex flex-col justify-between">
                <div>
                  <div className="text-[11px] font-semibold text-neutral-900 truncate" title={media.originalFilename}>
                    {media.originalFilename}
                  </div>
                  <div className="text-[10px] text-neutral-500 truncate mt-0.5">
                    {media.hotelName ? (
                      <span>{media.hotelName}{media.referenceName ? ` • ${media.referenceName}` : ''}</span>
                    ) : (
                      <span className="text-neutral-500 font-medium">Toàn chuỗi</span>
                    )}
                  </div>
                </div>

                <div className="mt-2 pt-2 border-t border-neutral-100 flex items-center justify-between text-[10px] text-neutral-400">
                  <span className="px-1.5 py-0.5 bg-neutral-100 rounded text-neutral-600 font-medium">
                    {media.category.replace('HOTEL_', '')}
                  </span>
                  <span>{new Date(media.createdAt).toLocaleDateString('vi-VN')}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* ── TABLE VIEW ── */
        <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-neutral-200 bg-neutral-50/75 text-neutral-600 text-xs font-semibold uppercase">
                  <th className="px-4 py-3.5">Ảnh</th>
                  <th className="px-4 py-3.5">Tên Tệp Tin</th>
                  <th className="px-4 py-3.5">Danh Mục</th>
                  <th className="px-4 py-3.5">Khách Sạn</th>
                  <th className="px-4 py-3.5">Dung Lượng</th>
                  <th className="px-4 py-3.5">Trạng Thái</th>
                  <th className="px-4 py-3.5 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {mediaList.map((media) => (
                  <tr key={media.id} className="hover:bg-neutral-50/70 transition-colors">
                    <td className="px-4 py-3">
                      <div
                        onClick={() => setDetailMedia(media)}
                        className="w-12 h-12 rounded-xl bg-neutral-100 overflow-hidden border border-neutral-200 cursor-pointer"
                      >
                        <img
                          src={media.url}
                          alt={media.originalFilename}
                          className="w-full h-full object-cover"
                        />
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-neutral-900 text-xs truncate max-w-xs">
                        {media.originalFilename}
                      </div>
                      <div className="text-[11px] font-mono text-neutral-400 truncate max-w-xs">
                        {media.storageKey}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-neutral-700">
                      <span className="px-2 py-0.5 rounded-full bg-neutral-100 font-medium">
                        {media.categoryLabel}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-neutral-700">
                      {media.hotelName ? (
                        <div>
                          <div className="font-medium text-neutral-900">{media.hotelName}</div>
                          {media.referenceName && (
                            <div className="text-[11px] text-red-600 font-semibold">{media.referenceName}</div>
                          )}
                        </div>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full bg-neutral-100 text-neutral-600 text-[11px] font-medium">
                          Toàn chuỗi
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-xs text-neutral-600 font-mono">
                      {formatBytes(media.sizeBytes)}
                    </td>
                    <td className="px-4 py-3">
                      {media.isPrimary ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-800 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                          <Star className="w-3 h-3 fill-current text-amber-600" />
                          Ảnh chính
                        </span>
                      ) : (
                        <span className="text-[11px] text-neutral-400">Ảnh thường</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="inline-flex items-center justify-end gap-1.5">
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => setDetailMedia(media)}
                          className="rounded-xl text-xs h-8 px-2.5"
                        >
                          Chi tiết
                        </Button>
                        <button
                          type="button"
                          onClick={() => setDeletingId(media.id)}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Xóa tệp"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Pagination */}
      {!loading && totalElements > 0 && (
        <div className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
          />
        </div>
      )}

      {/* Upload Modal */}
      {uploadModalOpen && (
        <MediaUploadModal
          isOpen={uploadModalOpen}
          onClose={() => setUploadModalOpen(false)}
          onSuccess={() => void loadMedia(true)}
          categories={categories}
          hotels={hotels}
        />
      )}

      {/* Detail Modal */}
      {detailMedia && (
        <MediaDetailModal
          media={detailMedia}
          onClose={() => setDetailMedia(null)}
          onSetPrimary={handleSetPrimary}
          onDelete={async (id) => {
            await mediaService.delete(id);
            await loadMedia(true);
          }}
        />
      )}

      {/* Delete Confirmation Modal */}
      {deletingId && (
        <ConfirmModal
          isOpen={Boolean(deletingId)}
          onClose={() => setDeletingId(null)}
          onConfirm={handleDeleteMedia}
          isLoading={isDeleting}
          variant="danger"
          title="Xác nhận xóa tệp hình ảnh"
          description="Bạn có chắc chắn muốn xóa tệp này khỏi hệ thống lưu trữ? Hành động này không thể hoàn tác."
          confirmText="Xác nhận Xóa"
          cancelText="Hủy bỏ"
        />
      )}
    </div>
  );
}

