import React from 'react';
import { createRoot } from 'react-dom/client';
import '../../src/index.css';
import { RoomAdvisorWidget } from '../../src/modules/booking-portal/components/RoomAdvisor/RoomAdvisorWidget';
createRoot(document.getElementById('root')!).render(<RoomAdvisorWidget />);
