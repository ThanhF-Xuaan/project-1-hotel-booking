import React from 'react';
import { createRoot } from 'react-dom/client';
import StayGuestsModal from '../../src/modules/booking/components/StayGuestsModal';
createRoot(document.getElementById('root')!).render(<StayGuestsModal isOpen onClose={() => {}} bookingRoomId={1} hotelId={1} roomId={1} roomNumber="101" />);
