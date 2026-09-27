export interface DashboardKpiResponse {
  hotelId: number;
  hotelName: string;
  targetDate: string;

  // Core Hotel Industry Metrics
  occupancyRate: number;        // OCC %
  averageDailyRate: number;     // ADR (VND)
  revPar: number;               // RevPAR (VND)

  // Inventory & Volume Metrics
  totalActiveRooms: number;
  occupiedRooms: number;
  availableRooms: number;
  outOfServiceRooms: number;

  // Revenue Stream
  totalRoomRevenue: number;
  totalServiceRevenue: number;
  totalRevenueToday: number;

  // Operations Quick Feed
  arrivalsTodayCount: number;
  departuresTodayCount: number;
  inHouseGuestsCount: number;
}

export interface RevenueTrendDto {
  date: string;
  roomRevenue: number;
  serviceRevenue: number;
  totalRevenue: number;
}

export interface OccupancyTrendDto {
  date: string;
  totalRooms: number;
  occupiedRooms: number;
  occupancyRate: number;
}
