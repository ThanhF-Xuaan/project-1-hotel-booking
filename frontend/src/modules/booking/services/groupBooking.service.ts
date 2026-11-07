import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  GroupContractResponse,
  GroupContractSearchDto,
  GroupContractCreateRequest,
  GroupContractDepositRequest,
  GroupContractCancelRequest,
  GroupRoomBlockResponse,
} from '../types/groupBooking.types';

const BASE_URL = '/api/v1/group-contracts';
const BLOCK_URL = '/api/v1/group-bookings';

export interface CompanyOption {
  id: number;
  companyName: string;
  taxCode: string;
  address?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
}

export interface CompanyRecord {
  id: number;
  name?: string;
  companyName?: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
}

export const groupBookingService = {
  /**
   * Tìm kiếm & phân trang danh sách hợp đồng khách đoàn (POST Search DTO)
   */
  filter: (searchDto: GroupContractSearchDto) =>
    apiClient.post<PageResponse<GroupContractResponse>>(`${BASE_URL}/filter`, searchDto),

  /**
   * Xuất danh sách hợp đồng khách đoàn ra tệp Excel (.xlsx)
   */
  exportExcel: (searchDto?: GroupContractSearchDto) =>
    apiClient.postBlob(`${BASE_URL}/export`, searchDto),

  /**
   * Xem chi tiết hợp đồng khách đoàn theo ID
   */
  getById: (id: number) =>
    apiClient.get<GroupContractResponse>(`${BASE_URL}/${id}`),

  /**
   * Tạo nhanh doanh nghiệp B2B mới vào CSDL
   */
  createCompany: (data: {
    name: string;
    taxCode?: string;
    address?: string;
    contactName?: string;
    contactPhone?: string;
    contactEmail?: string;
  }) =>
    apiClient.post<CompanyRecord>('/api/v1/companies', {
      ...data,
      status: 'ACTIVE',
    }),

  /**
   * Tạo mới hợp đồng khách đoàn (Báo giá & Giữ chỗ 3 ngày)
   */
  create: (data: GroupContractCreateRequest, staffId?: number) =>
    apiClient.post<GroupContractResponse>(
      staffId ? `${BASE_URL}?staffId=${staffId}` : BASE_URL,
      data
    ),

  /**
   * Xác nhận đặt cọc tối thiểu 50% theo Điều 328 BLDS 2015, kích hoạt block DEFINITE & sinh MasterFolio
   */
  confirmDeposit: (id: number, data: GroupContractDepositRequest) =>
    apiClient.post<GroupContractResponse>(`${BASE_URL}/${id}/deposit`, data),

  /**
   * Hủy hợp đồng khách đoàn và giải phóng block phòng
   */
  cancel: (id: number, data: GroupContractCancelRequest) =>
    apiClient.post<GroupContractResponse>(`${BASE_URL}/${id}/cancel`, data),

  /**
   * Xem danh sách Quỹ phòng đoàn (Room Blocks) của hợp đồng
   */
  getRoomBlocks: (contractId: number) =>
    apiClient.get<GroupRoomBlockResponse[]>(
      `${BLOCK_URL}/contracts/${contractId}/room-blocks`
    ),

  /**
   * Giải phóng thủ công các phòng chưa gán khách cho một Room Block về Public Inventory
   */
  releaseCutoff: (contractId: number, blockId: number) =>
    apiClient.post<GroupRoomBlockResponse>(
      `${BLOCK_URL}/contracts/${contractId}/room-blocks/${blockId}/release-cutoff`
    ),

  /**
   * Kích hoạt quét và giải phóng phòng quá hạn Cut-off Date
   */
  triggerCutoffAutoRelease: () =>
    apiClient.post<number>(`${BLOCK_URL}/scheduler/trigger-cutoff`),

  /**
   * Lấy danh sách doanh nghiệp đối tác (B2B Corporate Clients) từ API
   * Nếu DB chưa có công ty nào, tự động gọi API tạo các doanh nghiệp đối tác mẫu vào DB
   */
  getCompanies: async (): Promise<CompanyOption[]> => {
    try {
      const res = await apiClient.post<PageResponse<CompanyRecord>>('/api/v1/companies/filter', {
        page: 0,
        pageSize: 100,
        sortBy: 'id',
        sortDirection: 'ASC',
      });
      const list: CompanyOption[] = (res.result?.content || []).map((c: CompanyRecord) => ({
        id: c.id,
        companyName: c.name || c.companyName || `Doanh nghiệp #${c.id}`,
        taxCode: c.taxCode || '',
        address: c.address || '',
        contactPerson: c.contactName || c.contactPerson || '',
        contactPhone: c.contactPhone || '',
        contactEmail: c.contactEmail || '',
      }));

      if (list.length === 0) {
        const defaultCompanies = [
          {
            name: 'Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)',
            taxCode: '0100109106',
            address: 'Số 1 Trần Hữu Dực, Nam Từ Liêm, Hà Nội',
            contactName: 'Nguyễn Văn Đại',
            contactPhone: '02462556789',
            contactEmail: 'contact@viettel.com.vn',
          },
          {
            name: 'Công ty TNHH Phần mềm FPT (FPT Software)',
            taxCode: '0101778163',
            address: 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy, Hà Nội',
            contactName: 'Trần Minh Quân',
            contactPhone: '02473007300',
            contactEmail: 'contact@fsoft.com.vn',
          },
          {
            name: 'Tổng Công ty Du lịch Sài Gòn (Saigontourist)',
            taxCode: '0300625200',
            address: '23 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh',
            contactName: 'Lê Phương Thảo',
            contactPhone: '02838225887',
            contactEmail: 'saigontourist@saigontourist.com.vn',
          },
        ];

        for (const comp of defaultCompanies) {
          try {
            const created = await apiClient.post<CompanyRecord>('/api/v1/companies', comp);
            if (created.result) {
              list.push({
                id: created.result.id,
                companyName: created.result.name || '',
                taxCode: created.result.taxCode || '',
                address: created.result.address,
                contactPerson: created.result.contactName,
                contactPhone: created.result.contactPhone,
                contactEmail: created.result.contactEmail,
              });
            }
          } catch {
            // Không làm gián đoạn nếu gặp lỗi quyền
          }
        }
      }

      return list;
    } catch (err) {
      console.warn('Lỗi khi tải danh sách công ty từ API:', err);
      return [];
    }
  },
};
