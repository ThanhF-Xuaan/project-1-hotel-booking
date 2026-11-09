import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  CategoryMetaDto,
  MediaItemDto,
  MediaSearchDto,
  StorageCategory,
} from '../types/media.types';

export const mediaService = {
  filter: async (searchDto: MediaSearchDto): Promise<PageResponse<MediaItemDto>> => {
    const res = await apiClient.post<PageResponse<MediaItemDto>>(
      '/api/v1/media/filter',
      searchDto
    );
    return res.result;
  },

  upload: async (
    category: StorageCategory,
    files: File[],
    hotelId?: number,
    referenceId?: number
  ): Promise<MediaItemDto[]> => {
    const formData = new FormData();
    formData.append('category', category);
    if (hotelId) formData.append('hotelId', String(hotelId));
    if (referenceId) formData.append('referenceId', String(referenceId));
    files.forEach((file) => formData.append('files', file));

    const res = await apiClient.post<MediaItemDto[]>('/api/v1/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.result;
  },

  setPrimary: async (id: number): Promise<void> => {
    await apiClient.patch<void>(`/api/v1/media/${id}/primary`);
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete<void>(`/api/v1/media/${id}`);
  },

  getCategories: async (): Promise<CategoryMetaDto[]> => {
    const res = await apiClient.get<CategoryMetaDto[]>('/api/v1/media/categories');
    return res.result || [];
  },
};

export default mediaService;

