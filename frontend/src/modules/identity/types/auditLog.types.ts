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

export interface AuditLogSearchDto {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
  staffId?: number;
  actionType?: string;
  entityName?: string;
  entityId?: string;
  fromDate?: string;
  toDate?: string;
}
