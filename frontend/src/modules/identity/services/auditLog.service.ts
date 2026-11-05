import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type { AuditLogDto, AuditLogSearchDto } from '../types/auditLog.types';

const BASE_URL = '/api/v1/audit-logs';

export const auditLogService = {
  filter: (params: AuditLogSearchDto) =>
    apiClient.post<PageResponse<AuditLogDto>>(`${BASE_URL}/filter`, params),
};

export default auditLogService;

