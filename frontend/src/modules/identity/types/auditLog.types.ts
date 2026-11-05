import type { BaseSearchDto } from '../../../core/types/common.types';

export interface AuditLogDto {
  id: number;
  staffId?: number;
  staffUsername?: string;
  staffFullName?: string;
  actionType: string;
  entityName: string;
  entityId: string;
  oldValues?: string;
  newValues?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

export interface AuditLogSearchDto extends BaseSearchDto {
  staffId?: number;
  actionType?: string;
  entityName?: string;
  entityId?: string;
  fromDate?: string;
  toDate?: string;
}

