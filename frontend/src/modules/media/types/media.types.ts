export type StorageCategory =
  | 'HOTEL_PROPERTY_PHOTO'
  | 'HOTEL_ROOM_PHOTO'
  | 'HOTEL_AMENITY'
  | 'BRAND_ASSET'
  | 'MARKETING_BANNER'
  | 'USER_AVATAR'
  | 'GUEST_REVIEW_PHOTO'
  | 'SECURE_CONTRACT'
  | 'GUEST_IDENTITY'
  | 'FINANCIAL_RECEIPT'
  | 'SYSTEM_EXPORT';

export interface CategoryMetaDto {
  category: StorageCategory;
  label: string;
  folderPrefix: string;
  isPublic: boolean;
  maxSizeBytes: number;
  allowedMimeTypes: string[];
}

export interface MediaItemDto {
  id: number;
  storageKey: string;
  url: string;
  category: StorageCategory;
  categoryLabel: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256?: string;
  hotelId?: number | null;
  hotelName?: string | null;
  referenceId?: number | null;
  referenceName?: string | null;
  isPrimary: boolean;
  accessLevel: 'PUBLIC_READ' | 'AUTHENTICATED_READ' | 'PRIVATE_RESTRICTED';
  lifecycleStatus: string;
  uploadedBy?: string;
  createdAt: string;
}

export interface MediaSearchDto {
  keyword?: string;
  category?: StorageCategory | '';
  hotelId?: number | '';
  referenceId?: number | '';
  isPrimary?: boolean;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

