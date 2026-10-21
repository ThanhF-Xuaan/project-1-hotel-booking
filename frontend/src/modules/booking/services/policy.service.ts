import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  CancellationPolicyCreateRequest,
  CancellationPolicyResponse,
  CancellationPolicySearchDto,
  CancellationPolicyUpdateRequest,
  NoShowPolicyCreateRequest,
  NoShowPolicyResponse,
  NoShowPolicySearchDto,
  NoShowPolicyUpdateRequest,
  PropertyPolicyOverrideCreateRequest,
  PropertyPolicyOverrideResponse,
  PropertyPolicyOverrideSearchDto,
  PropertyPolicyOverrideUpdateRequest,
} from '../types/policy.types';

const CANCEL_BASE = '/api/v1/cancellation-policies';
const NOSHOW_BASE = '/api/v1/no-show-policies';
const OVERRIDE_BASE = '/api/v1/property-policy-overrides';

export const policyService = {
  // === Cancellation Policies ===
  filterCancellationPolicies: (params: CancellationPolicySearchDto) =>
    apiClient.post<PageResponse<CancellationPolicyResponse>>(`${CANCEL_BASE}/filter`, params),

  getCancellationPolicyById: (id: number) =>
    apiClient.get<CancellationPolicyResponse>(`${CANCEL_BASE}/${id}`),

  createCancellationPolicy: (data: CancellationPolicyCreateRequest) =>
    apiClient.post<CancellationPolicyResponse>(CANCEL_BASE, data),

  updateCancellationPolicy: (id: number, data: CancellationPolicyUpdateRequest) =>
    apiClient.put<CancellationPolicyResponse>(`${CANCEL_BASE}/${id}`, data),

  deleteCancellationPolicies: (ids: number[]) =>
    apiClient.delete<void>(`${CANCEL_BASE}/delete`, { data: ids }),

  // === No-Show Policies ===
  filterNoShowPolicies: (params: NoShowPolicySearchDto) =>
    apiClient.post<PageResponse<NoShowPolicyResponse>>(`${NOSHOW_BASE}/filter`, params),

  getNoShowPolicyById: (id: number) =>
    apiClient.get<NoShowPolicyResponse>(`${NOSHOW_BASE}/${id}`),

  createNoShowPolicy: (data: NoShowPolicyCreateRequest) =>
    apiClient.post<NoShowPolicyResponse>(NOSHOW_BASE, data),

  updateNoShowPolicy: (id: number, data: NoShowPolicyUpdateRequest) =>
    apiClient.put<NoShowPolicyResponse>(`${NOSHOW_BASE}/${id}`, data),

  deleteNoShowPolicies: (ids: number[]) =>
    apiClient.delete<void>(`${NOSHOW_BASE}/delete`, { data: ids }),

  // === Property Policy Overrides ===
  filterPropertyOverrides: (params: PropertyPolicyOverrideSearchDto) =>
    apiClient.post<PageResponse<PropertyPolicyOverrideResponse>>(`${OVERRIDE_BASE}/filter`, params),

  getPropertyOverrideById: (id: number) =>
    apiClient.get<PropertyPolicyOverrideResponse>(`${OVERRIDE_BASE}/${id}`),

  createPropertyOverride: (data: PropertyPolicyOverrideCreateRequest) =>
    apiClient.post<PropertyPolicyOverrideResponse>(OVERRIDE_BASE, data),

  updatePropertyOverride: (id: number, data: PropertyPolicyOverrideUpdateRequest) =>
    apiClient.put<PropertyPolicyOverrideResponse>(`${OVERRIDE_BASE}/${id}`, data),

  deletePropertyOverrides: (ids: number[]) =>
    apiClient.delete<void>(`${OVERRIDE_BASE}/delete`, { data: ids }),
};

export default policyService;
