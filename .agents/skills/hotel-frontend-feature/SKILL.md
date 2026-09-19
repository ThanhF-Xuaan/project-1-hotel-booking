---
name: hotel-frontend-feature
description: Step-by-step guide for building a new React page or feature in the hotel booking system frontend (React 19 + TypeScript + Vite + Tailwind CSS v4 + react-hook-form + zod). Use this skill when creating new pages, components, hooks, or API service integrations.
---

# Hotel Frontend Feature Development Skill

This skill provides structured guidance for adding a new UI feature or page to the hotel booking system React frontend.

## Frontend Source Structure

```
frontend/src/
├── components/        # Reusable UI components (shared across pages)
│   ├── ui/            # Base primitives (Button, Input, Modal, Badge, etc.)
│   ├── booking/       # Booking-specific components
│   ├── room/          # Room display components
│   └── layout/        # Header, Sidebar, Footer, PageWrapper
├── pages/             # Route-level page components
│   ├── admin/         # Admin dashboard pages
│   ├── booking/       # Guest booking flow pages
│   └── auth/          # Login/callback pages
├── hooks/             # Custom React hooks (useBooking, useRooms, etc.)
├── services/          # API call functions organized by domain
│   ├── bookingService.ts
│   ├── roomService.ts
│   └── pricingService.ts
├── types/             # TypeScript interfaces for API response shapes
│   ├── booking.ts
│   ├── room.ts
│   └── pricing.ts
├── lib/               # Configured library instances
│   ├── axios.ts       # Axios instance with auth interceptor
│   └── keycloak.ts    # Keycloak JS adapter config
├── utils/             # Pure utility functions
│   └── date-utils.ts  # date-fns wrappers
├── router/            # React Router v6 configuration
└── App.tsx
```

## Step-by-Step: Building a New Feature

### Step 1: Define API Response Types

In `src/types/<domain>.ts`, define TypeScript interfaces matching the backend DTO shapes:

```typescript
// src/types/room.ts
export interface RoomType {
  id: number;
  name: string;
  capacity: number;
  basePrice: number;
  amenities: string[];
  status: 'ACTIVE' | 'INACTIVE';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}
```

### Step 2: Create the API Service Function

In `src/services/<domain>Service.ts`:

```typescript
// src/services/roomService.ts
import { api } from '@/lib/axios';
import type { RoomType, PageResponse } from '@/types/room';

export interface FetchRoomTypesParams {
  hotelId: number;
  page?: number;
  size?: number;
}

export const roomService = {
  async getRoomTypes(params: FetchRoomTypesParams): Promise<PageResponse<RoomType>> {
    const { data } = await api.get<PageResponse<RoomType>>('/room-types', { params });
    return data;
  },

  async getRoomTypeById(id: number): Promise<RoomType> {
    const { data } = await api.get<RoomType>(`/room-types/${id}`);
    return data;
  },
};
```

### Step 3: Create a Custom Data Hook

In `src/hooks/use<Feature>.ts`:

```typescript
// src/hooks/useRoomTypes.ts
import { useState, useEffect, useCallback } from 'react';
import { roomService, type FetchRoomTypesParams } from '@/services/roomService';
import type { RoomType, PageResponse } from '@/types/room';

export function useRoomTypes(params: FetchRoomTypesParams) {
  const [data, setData] = useState<PageResponse<RoomType> | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await roomService.getRoomTypes(params);
      setData(result);
    } catch (err) {
      setError('Không thể tải danh sách phòng. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [params.hotelId, params.page, params.size]);

  useEffect(() => { fetchData(); }, [fetchData]);

  return { data, isLoading, error, refetch: fetchData };
}
```

### Step 4: Build Reusable Components

For domain-specific UI atoms in `src/components/<domain>/`:

```tsx
// src/components/room/RoomTypeCard.tsx
import type { RoomType } from '@/types/room';

interface RoomTypeCardProps {
  roomType: RoomType;
  onSelect: (id: number) => void;
}

export function RoomTypeCard({ roomType, onSelect }: RoomTypeCardProps) {
  return (
    <div className="rounded-xl border border-neutral-200 p-4 hover:shadow-md transition-shadow">
      <h3 className="text-lg font-semibold">{roomType.name}</h3>
      <p className="text-sm text-neutral-500">Tối đa {roomType.capacity} khách</p>
      <button
        type="button"
        onClick={() => onSelect(roomType.id)}
        className="mt-3 w-full rounded-lg bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        Chọn phòng
      </button>
    </div>
  );
}
```

### Step 5: Build Forms with react-hook-form + zod

```tsx
// src/components/booking/BookingForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const bookingSchema = z.object({
  checkInDate: z.string().min(1, 'Vui lòng chọn ngày nhận phòng'),
  checkOutDate: z.string().min(1, 'Vui lòng chọn ngày trả phòng'),
  guestCount: z.number().min(1).max(10),
}).refine(data => data.checkOutDate > data.checkInDate, {
  message: 'Ngày trả phòng phải sau ngày nhận phòng',
  path: ['checkOutDate'],
});

type BookingFormValues = z.infer<typeof bookingSchema>;

export function BookingForm({ onSubmit }: { onSubmit: (values: BookingFormValues) => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<BookingFormValues>({
    resolver: zodResolver(bookingSchema),
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <label htmlFor="checkInDate" className="block text-sm font-medium">Ngày nhận phòng</label>
        <input id="checkInDate" type="date" {...register('checkInDate')}
          className="mt-1 block w-full rounded-md border px-3 py-2" />
        {errors.checkInDate && <p className="mt-1 text-xs text-red-500">{errors.checkInDate.message}</p>}
      </div>
      {/* similar for checkOutDate, guestCount */}
      <button type="submit" disabled={isSubmitting}
        className="w-full rounded-lg bg-blue-600 py-2 text-white disabled:opacity-50">
        {isSubmitting ? 'Đang xử lý...' : 'Xác nhận đặt phòng'}
      </button>
    </form>
  );
}
```

### Step 6: Create the Page Component

In `src/pages/<domain>/<PageName>.tsx`:
- Compose hooks + components together
- Handle loading states with a `<LoadingSpinner />` component
- Handle error states with an inline error message + retry button
- Add to router in `src/router/index.tsx` with proper lazy loading

### Step 7: Add Routing

```tsx
// src/router/index.tsx — add lazy route
const RoomListPage = lazy(() => import('@/pages/room/RoomListPage'));

{
  path: '/hotels/:hotelId/rooms',
  element: (
    <Suspense fallback={<LoadingSpinner />}>
      <RoomListPage />
    </Suspense>
  ),
}
```

## Admin Dashboard Features

For admin pages (Timeline Matrix, Occupancy Reports):
- Admin routes must be wrapped in `<ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_RECEPTIONIST']} />`
- The Availability Matrix (calendar grid) is a complex component — place in `src/components/availability/`
- Charts for Occupancy Rate use a charting library — render data from `/api/analytics/occupancy` endpoint

## Common Pitfalls

- Never call `api.get()` directly inside a component — always go through a service function
- Never access Keycloak token directly in components — use the axios interceptor
- Check-in/Check-out dates: pass as `YYYY-MM-DD` strings; display using `date-fns` with `vi` locale
- Loading state: always show a skeleton or spinner while fetching; never render empty UI without feedback
- Price display: format using `Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })`
