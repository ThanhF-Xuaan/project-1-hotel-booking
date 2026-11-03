import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import StayGuestsModal from '../../src/modules/booking/components/StayGuestsModal';
export default function Fixture() {
  const [room, setRoom] = useState(1);
  const [hotel, setHotel] = useState(1);
  const [open, setOpen] = useState(true);
  return <><div style={{ position: 'fixed', zIndex: 9999, top: 0 }}>
    <button onClick={() => setRoom(room === 1 ? 2 : 1)}>Switch room</button>
    <button onClick={() => setHotel(hotel === 1 ? 2 : 1)}>Switch hotel</button>
    <button onClick={() => setOpen(!open)}>Toggle modal</button>
  </div><StayGuestsModal isOpen={open} onClose={() => setOpen(false)} bookingRoomId={room}
    hotelId={hotel} bookingId={10} roomId={room} roomNumber={String(100 + room)} /></>;
}
createRoot(document.getElementById('root')!).render(<Fixture />);
