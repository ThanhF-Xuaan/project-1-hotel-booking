export type MenuType = 'PRODUCT' | 'SERVICE';

export type ServicePricingType = 'PER_STAY' | 'PER_NIGHT' | 'PER_PERSON' | 'PER_UNIT';

export type ServiceOrderStatus = 'PENDING' | 'PREPARING' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED';

export interface MenuCreateRequest {
  hotelId?: number;
  taxCategoryId: number;
  menuType: MenuType;
  name: string;
  description?: string;
  basePrice: number;
  stockQuantity?: number;
  pricingType?: ServicePricingType;
}

export interface MenuUpdateRequest {
  taxCategoryId?: number;
  name?: string;
  description?: string;
  basePrice?: number;
  status?: string;
  stockQuantity?: number;
  pricingType?: ServicePricingType;
}

export interface MenuSearchDto {
  hotelId?: number;
  menuType?: MenuType;
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface MenuResponse {
  id: number;
  hotelId?: number;
  hotelName?: string;
  taxCategoryId: number;
  taxCategoryName?: string;
  menuType: MenuType;
  name: string;
  description?: string;
  basePrice: number;
  status: string;
  stockQuantity?: number;
  pricingType?: ServicePricingType;
}

export interface OrderItemRequest {
  menuId: number;
  quantity: number;
}

export interface ServiceOrderCreateRequest {
  bookingId?: number;
  roomInstanceId: number;
  items: OrderItemRequest[];
}

export interface ServiceOrderSearchDto {
  bookingId?: number;
  roomInstanceId?: number;
  orderNumber?: string;
  status?: ServiceOrderStatus;
  page?: number;
  pageSize?: number;
}

export interface ServiceOrderDetailResponse {
  id: number;
  menuId: number;
  itemType: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  totalAmount: number;
}

export interface ServiceOrderResponse {
  id: number;
  orderNumber: string;
  bookingId: number;
  bookingNumber?: string;
  roomInstanceId: number;
  roomNumber?: string;
  subTotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatAmount: number;
  totalAmount: number;
  status: ServiceOrderStatus;
  issuedAt: string;
  createdAt: string;
  details: ServiceOrderDetailResponse[];
}

export interface RoomHousekeepingStatusResponse {
  roomInstanceId: number;
  roomNumber: string;
  hotelRoomTypeId?: number;
  roomTypeName?: string;
  hotelId: number;
  hotelName?: string;
  currentStatus: string; // READY, OCCUPIED, CLEANING, DIRTY, MAINTENANCE
  guestName?: string;
  bookingNumber?: string;
  checkOutDate?: string;
}

export interface RoomStatusUpdateRequest {
  status: string;
}

export interface NightAuditRequest {
  hotelId: number;
  auditDate: string;
}

export interface NightAuditResponse {
  hotelId: number;
  auditDate: string;
  noShowBookingsCount: number;
  occupiedRoomsCount: number;
  totalDailyRoomRevenue: number;
  totalDailyServiceRevenue: number;
  totalDailyRevenue: number;
  auditTimestamp: string;
  status: string;
}
