export interface CompanyDto {
  id: number;
  name: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
}

export interface CompanyCreateRequest {
  name: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  status?: string;
}

export interface CompanyUpdateRequest {
  name: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  status: string;
}

export interface CompanySearchDto {
  page?: number;
  pageSize?: number;
  keyword?: string;
  taxCode?: string;
  status?: string;
}

