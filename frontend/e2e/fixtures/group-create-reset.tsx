import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { GroupCreateContractModal } from '../../src/modules/booking/components/GroupCreateContractModal';

export default function Fixture() {
  const [openKeptMounted, setOpenKeptMounted] = useState(false);
  const [openUnmounted, setOpenUnmounted] = useState(false);

  return (
    <div style={{ padding: 20 }}>
      <button id="btn-open-kept" onClick={() => setOpenKeptMounted(true)}>
        Open Kept Mounted
      </button>
      <button id="btn-open-unmounted" onClick={() => setOpenUnmounted(true)}>
        Open Unmounted
      </button>

      {/* Case 1: Modal kept mounted in DOM when closed (props isOpen=false) */}
      <GroupCreateContractModal
        isOpen={openKeptMounted}
        onClose={() => setOpenKeptMounted(false)}
        onSubmit={async () => {}}
      />

      {/* Case 2: Modal conditionally rendered in DOM */}
      {openUnmounted && (
        <GroupCreateContractModal
          isOpen={openUnmounted}
          onClose={() => setOpenUnmounted(false)}
          onSubmit={async () => {}}
        />
      )}
    </div>
  );
}

const rootEl = document.getElementById('root');
if (rootEl) {
  createRoot(rootEl).render(<Fixture />);
}
